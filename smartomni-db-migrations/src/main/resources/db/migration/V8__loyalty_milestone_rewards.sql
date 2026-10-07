-- One point per complete VND 1,000 of the order total. The trigger snapshots
-- the amount and calculates points; callers cannot choose an award amount.
ALTER TABLE loyalty_claims ADD COLUMN eligible_amount_vnd numeric(14,2);

CREATE OR REPLACE FUNCTION loyalty_check_claim_eligibility() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  order_row orders%ROWTYPE;
BEGIN
  SELECT * INTO order_row FROM orders
   WHERE tenant_id = NEW.tenant_id AND id = NEW.order_id FOR SHARE;
  IF NOT FOUND OR order_row.status IS DISTINCT FROM 'completed'
      OR order_row.eligible_for_points_at IS NULL
      OR order_row.eligible_for_points_at > now()
      OR order_row.total_amount IS NULL OR order_row.total_amount < 1000 THEN
    RAISE EXCEPTION 'Order is not eligible for loyalty points'
      USING ERRCODE = '23514';
  END IF;
  NEW.eligible_amount_vnd := order_row.total_amount;
  NEW.points_awarded := floor(order_row.total_amount / 1000)::bigint;
  RETURN NEW;
END $$;

-- Existing balances may include redemptions from V7. Under milestone rewards,
-- only earned points (net of reversals) count toward tiers and eligibility.
ALTER TABLE loyalty_point_accounts RENAME COLUMN balance TO total_points;
UPDATE loyalty_point_accounts a SET total_points = COALESCE((
  SELECT sum(l.points_delta) FROM loyalty_point_ledger l
   WHERE l.tenant_id = a.tenant_id AND l.customer_id = a.customer_id
     AND l.reason IN ('earn', 'reverse')
), 0);
ALTER TABLE loyalty_point_accounts ADD COLUMN tier varchar
  GENERATED ALWAYS AS (
    CASE WHEN total_points >= 1500 THEN 'diamond'
         WHEN total_points >= 1000 THEN 'platinum'
         WHEN total_points >= 500 THEN 'silver'
         ELSE 'standard' END
  ) STORED;

CREATE OR REPLACE FUNCTION loyalty_apply_point_delta() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
  IF NEW.reason = 'redeem' THEN
    RAISE EXCEPTION 'Redeeming a milestone reward does not consume points'
      USING ERRCODE = '23514';
  END IF;
  INSERT INTO loyalty_point_accounts (tenant_id, customer_id, total_points)
  VALUES (NEW.tenant_id, NEW.customer_id, NEW.points_delta)
  ON CONFLICT (tenant_id, customer_id) DO UPDATE
    SET total_points = loyalty_point_accounts.total_points + EXCLUDED.total_points,
        updated_at = now();
  RETURN NEW;
END $$;

-- V7's direct voucher issuance debits points; retire that write path.
REVOKE INSERT ON loyalty_voucher_issuances FROM smartomni_app;
REVOKE INSERT ON loyalty_voucher_codes FROM smartomni_app;

CREATE TABLE loyalty_reward_campaigns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  connection_id bigint NOT NULL,
  created_by bigint NOT NULL,
  name varchar NOT NULL,
  discount_percent numeric(5,2) NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 100),
  required_points bigint NOT NULL CHECK (required_points > 0),
  quantity integer NOT NULL CHECK (quantity > 0),
  reserved_count integer NOT NULL DEFAULT 0 CHECK (reserved_count >= 0 AND reserved_count <= quantity),
  provisioning_mode varchar NOT NULL CHECK (provisioning_mode IN ('api', 'import')),
  status varchar NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'paused', 'closed')),
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  CONSTRAINT ck_reward_campaign_dates CHECK (ends_at > starts_at),
  CONSTRAINT uq_reward_campaigns_tenant_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_reward_campaign_connection FOREIGN KEY (tenant_id, connection_id)
    REFERENCES marketplace_connections (tenant_id, id),
  CONSTRAINT fk_reward_campaign_staff FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_reward_campaigns_available
  ON loyalty_reward_campaigns (tenant_id, required_points, ends_at)
  WHERE status = 'active';

CREATE FUNCTION loyalty_check_reward_campaign() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE shop_platform platform_type;
BEGIN
  SELECT platform INTO shop_platform FROM marketplace_connections
   WHERE tenant_id = NEW.tenant_id AND id = NEW.connection_id;
  IF shop_platform IS NULL OR shop_platform NOT IN ('shopee', 'tiktok_shop') THEN
    RAISE EXCEPTION 'Reward campaign requires a Shopee or TikTok shop'
      USING ERRCODE = '23514';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM users WHERE id = NEW.created_by
      AND tenant_id = NEW.tenant_id AND role IN ('admin', 'manager')) THEN
    RAISE EXCEPTION 'Reward campaign creator must be tenant staff'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_check_reward_campaign
  BEFORE INSERT OR UPDATE OF tenant_id, connection_id, created_by
  ON loyalty_reward_campaigns FOR EACH ROW
  EXECUTE FUNCTION loyalty_check_reward_campaign();

-- A pending request reserves one unit before any network call. The adapter
-- later records the marketplace code, or fails and releases the reservation.
CREATE TABLE loyalty_reward_requests (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  campaign_id bigint NOT NULL,
  customer_id bigint NOT NULL,
  request_key uuid NOT NULL DEFAULT gen_random_uuid(),
  status varchar NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending', 'issued', 'failed')),
  code varchar,
  external_voucher_id varchar,
  failure_reason text,
  requested_at timestamptz NOT NULL DEFAULT now(),
  issued_at timestamptz,
  updated_at timestamptz,
  CONSTRAINT ck_reward_request_result CHECK (
    (status = 'issued' AND code IS NOT NULL AND issued_at IS NOT NULL)
    OR (status IN ('pending', 'failed') AND code IS NULL AND issued_at IS NULL)
  ),
  CONSTRAINT uq_reward_requests_tenant_id UNIQUE (tenant_id, id),
  CONSTRAINT uq_reward_requests_key UNIQUE (request_key),
  CONSTRAINT fk_reward_request_campaign FOREIGN KEY (tenant_id, campaign_id)
    REFERENCES loyalty_reward_campaigns (tenant_id, id),
  CONSTRAINT fk_reward_request_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customer_accounts (tenant_id, id)
);
CREATE UNIQUE INDEX uq_reward_requests_customer_campaign_active
  ON loyalty_reward_requests (tenant_id, campaign_id, customer_id)
  WHERE status IN ('pending', 'issued');
CREATE INDEX idx_reward_requests_pending
  ON loyalty_reward_requests (requested_at) WHERE status = 'pending';

CREATE FUNCTION loyalty_reserve_reward() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE threshold bigint;
BEGIN
  IF NEW.status <> 'pending' OR NEW.code IS NOT NULL THEN
    RAISE EXCEPTION 'New reward request must be pending' USING ERRCODE = '23514';
  END IF;
  UPDATE loyalty_reward_campaigns
     SET reserved_count = reserved_count + 1, updated_at = now()
   WHERE id = NEW.campaign_id AND tenant_id = NEW.tenant_id
     AND status = 'active' AND starts_at <= now() AND ends_at > now()
     AND reserved_count < quantity
   RETURNING required_points INTO threshold;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Reward unavailable or quota exhausted' USING ERRCODE = '23514';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM loyalty_point_accounts
      WHERE tenant_id = NEW.tenant_id AND customer_id = NEW.customer_id
        AND total_points >= threshold) THEN
    RAISE EXCEPTION 'Customer has not reached reward threshold' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_reserve_reward BEFORE INSERT ON loyalty_reward_requests
  FOR EACH ROW EXECUTE FUNCTION loyalty_reserve_reward();

CREATE FUNCTION loyalty_finish_reward() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
  IF OLD.status <> 'pending' OR NEW.tenant_id <> OLD.tenant_id
      OR NEW.campaign_id <> OLD.campaign_id OR NEW.customer_id <> OLD.customer_id
      OR NEW.request_key <> OLD.request_key
      OR NEW.status NOT IN ('issued', 'failed') THEN
    RAISE EXCEPTION 'Invalid reward request transition' USING ERRCODE = '23514';
  END IF;
  IF NEW.status = 'failed' THEN
    UPDATE loyalty_reward_campaigns SET reserved_count = reserved_count - 1,
      updated_at = now() WHERE tenant_id = NEW.tenant_id AND id = NEW.campaign_id;
  END IF;
  NEW.updated_at := now();
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_finish_reward BEFORE UPDATE ON loyalty_reward_requests
  FOR EACH ROW EXECUTE FUNCTION loyalty_finish_reward();

DO $$
DECLARE table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY['loyalty_reward_campaigns', 'loyalty_reward_requests'] LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
    EXECUTE format(
      'CREATE POLICY tenant_isolation ON %I USING (tenant_id = app_current_tenant_id() OR app_is_super_admin()) WITH CHECK (tenant_id = app_current_tenant_id() OR app_is_super_admin())',
      table_name);
  END LOOP;
END $$;

GRANT SELECT, INSERT, UPDATE ON loyalty_reward_campaigns TO smartomni_app;
GRANT SELECT, INSERT ON loyalty_reward_requests TO smartomni_app;
GRANT UPDATE (status, code, external_voucher_id, failure_reason, issued_at)
  ON loyalty_reward_requests TO smartomni_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO smartomni_app;

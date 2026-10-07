-- Customer loyalty is tenant-local. Customers are separate from staff users.
-- An order is awarded to the first authenticated customer who claims it;
-- ownership of the marketplace order is intentionally not verified.
ALTER TABLE orders
  ADD COLUMN completed_at timestamptz,
  ADD COLUMN eligible_for_points_at timestamptz,
  ADD CONSTRAINT ck_orders_loyalty_dates CHECK (
    eligible_for_points_at IS NULL OR
    (completed_at IS NOT NULL AND eligible_for_points_at >= completed_at)
  ),
  ADD CONSTRAINT uq_orders_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE marketplace_connections
  ADD CONSTRAINT uq_marketplace_connections_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE customer_accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  email varchar NOT NULL,
  password_hash varchar NOT NULL,
  display_name varchar,
  status varchar NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'disabled')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  CONSTRAINT uq_customer_accounts_tenant_id UNIQUE (tenant_id, id)
);
CREATE UNIQUE INDEX uq_customer_accounts_tenant_email
  ON customer_accounts (tenant_id, lower(email));

CREATE TABLE loyalty_claims (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  order_id bigint NOT NULL,
  customer_id bigint NOT NULL,
  points_awarded bigint NOT NULL CHECK (points_awarded > 0),
  claimed_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_loyalty_claims_tenant_id UNIQUE (tenant_id, id),
  CONSTRAINT uq_loyalty_claims_order UNIQUE (tenant_id, order_id),
  CONSTRAINT fk_loyalty_claims_order FOREIGN KEY (tenant_id, order_id)
    REFERENCES orders (tenant_id, id),
  CONSTRAINT fk_loyalty_claims_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customer_accounts (tenant_id, id)
);
CREATE INDEX idx_loyalty_claims_customer
  ON loyalty_claims (tenant_id, customer_id, claimed_at DESC);

CREATE FUNCTION loyalty_check_claim_eligibility() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  order_status_value order_status;
  eligible_at timestamptz;
BEGIN
  SELECT o.status, o.eligible_for_points_at
    INTO order_status_value, eligible_at
    FROM orders o
    WHERE o.tenant_id = NEW.tenant_id AND o.id = NEW.order_id
    FOR SHARE;
  IF NOT FOUND OR order_status_value IS DISTINCT FROM 'completed'
      OR eligible_at IS NULL OR eligible_at > now() THEN
    RAISE EXCEPTION 'Order is not eligible for loyalty points'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_check_claim_eligibility
  BEFORE INSERT ON loyalty_claims
  FOR EACH ROW EXECUTE FUNCTION loyalty_check_claim_eligibility();

CREATE TABLE loyalty_point_accounts (
  tenant_id bigint NOT NULL,
  customer_id bigint NOT NULL,
  balance bigint NOT NULL DEFAULT 0,
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, customer_id),
  CONSTRAINT fk_loyalty_point_accounts_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customer_accounts (tenant_id, id)
);

-- Voucher codes belong to exactly one connected shop on one marketplace.
-- A local issuance does not prove the code was used on the marketplace.
CREATE TABLE loyalty_voucher_codes (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  connection_id bigint NOT NULL,
  code varchar NOT NULL,
  points_cost bigint NOT NULL CHECK (points_cost > 0),
  status varchar NOT NULL DEFAULT 'available'
    CHECK (status IN ('available', 'issued', 'disabled')),
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_loyalty_voucher_codes_tenant_id UNIQUE (tenant_id, id),
  CONSTRAINT uq_loyalty_voucher_codes_shop_code UNIQUE (tenant_id, connection_id, code),
  CONSTRAINT fk_loyalty_voucher_codes_connection FOREIGN KEY (tenant_id, connection_id)
    REFERENCES marketplace_connections (tenant_id, id)
);
CREATE INDEX idx_loyalty_voucher_codes_available
  ON loyalty_voucher_codes (tenant_id, connection_id, points_cost, expires_at)
  WHERE status = 'available';

CREATE FUNCTION loyalty_check_voucher_shop() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  shop_platform platform_type;
BEGIN
  SELECT platform INTO shop_platform FROM marketplace_connections
    WHERE tenant_id = NEW.tenant_id AND id = NEW.connection_id;
  IF NOT FOUND OR shop_platform NOT IN ('shopee', 'tiktok_shop') THEN
    RAISE EXCEPTION 'Voucher must belong to a Shopee or TikTok shop'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_check_voucher_shop
  BEFORE INSERT OR UPDATE OF tenant_id, connection_id ON loyalty_voucher_codes
  FOR EACH ROW EXECUTE FUNCTION loyalty_check_voucher_shop();

CREATE TABLE loyalty_voucher_issuances (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_id bigint NOT NULL,
  voucher_code_id bigint NOT NULL UNIQUE,
  points_spent bigint NOT NULL CHECK (points_spent > 0),
  issued_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_loyalty_voucher_issuances_tenant_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_loyalty_voucher_issuances_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customer_accounts (tenant_id, id),
  CONSTRAINT fk_loyalty_voucher_issuances_code FOREIGN KEY (tenant_id, voucher_code_id)
    REFERENCES loyalty_voucher_codes (tenant_id, id)
);
CREATE INDEX idx_loyalty_voucher_issuances_customer
  ON loyalty_voucher_issuances (tenant_id, customer_id, issued_at DESC);

CREATE TABLE loyalty_point_ledger (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_id bigint NOT NULL,
  claim_id bigint,
  voucher_issuance_id bigint,
  reason varchar NOT NULL CHECK (reason IN ('earn', 'redeem', 'reverse')),
  points_delta bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_loyalty_point_ledger_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customer_accounts (tenant_id, id),
  CONSTRAINT fk_loyalty_point_ledger_claim FOREIGN KEY (tenant_id, claim_id)
    REFERENCES loyalty_claims (tenant_id, id),
  CONSTRAINT fk_loyalty_point_ledger_issuance FOREIGN KEY (tenant_id, voucher_issuance_id)
    REFERENCES loyalty_voucher_issuances (tenant_id, id),
  CONSTRAINT ck_loyalty_point_ledger_source CHECK (
    (reason = 'earn' AND claim_id IS NOT NULL AND voucher_issuance_id IS NULL AND points_delta > 0)
    OR (reason = 'reverse' AND claim_id IS NOT NULL AND voucher_issuance_id IS NULL AND points_delta < 0)
    OR (reason = 'redeem' AND claim_id IS NULL AND voucher_issuance_id IS NOT NULL AND points_delta < 0)
  )
);
CREATE UNIQUE INDEX uq_loyalty_point_ledger_claim_reason
  ON loyalty_point_ledger (tenant_id, claim_id, reason) WHERE claim_id IS NOT NULL;
CREATE UNIQUE INDEX uq_loyalty_point_ledger_issuance
  ON loyalty_point_ledger (tenant_id, voucher_issuance_id)
  WHERE voucher_issuance_id IS NOT NULL;
CREATE INDEX idx_loyalty_point_ledger_customer
  ON loyalty_point_ledger (tenant_id, customer_id, created_at DESC);

-- Updating the cached balance in the ledger transaction makes concurrent
-- redemptions safe. Refund reversals may create a negative balance (debt).
CREATE FUNCTION loyalty_apply_point_delta() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
  IF NEW.reason = 'redeem' THEN
    UPDATE loyalty_point_accounts
       SET balance = balance + NEW.points_delta, updated_at = now()
     WHERE tenant_id = NEW.tenant_id AND customer_id = NEW.customer_id
       AND balance + NEW.points_delta >= 0;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'Insufficient loyalty points'
        USING ERRCODE = '23514';
    END IF;
  ELSE
    INSERT INTO loyalty_point_accounts (tenant_id, customer_id, balance)
    VALUES (NEW.tenant_id, NEW.customer_id, NEW.points_delta)
    ON CONFLICT (tenant_id, customer_id) DO UPDATE
      SET balance = loyalty_point_accounts.balance + EXCLUDED.balance,
          updated_at = now();
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_apply_point_delta
  AFTER INSERT ON loyalty_point_ledger
  FOR EACH ROW EXECUTE FUNCTION loyalty_apply_point_delta();

-- The application creates a claim or issuance; the database records the
-- corresponding points movement so neither operation can omit the ledger.
CREATE FUNCTION loyalty_record_claim_points() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO loyalty_point_ledger
    (tenant_id, customer_id, claim_id, reason, points_delta)
  VALUES (NEW.tenant_id, NEW.customer_id, NEW.id, 'earn', NEW.points_awarded);
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_record_claim_points
  AFTER INSERT ON loyalty_claims
  FOR EACH ROW EXECUTE FUNCTION loyalty_record_claim_points();

CREATE FUNCTION loyalty_issue_voucher() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  code_cost bigint;
BEGIN
  SELECT points_cost INTO code_cost
    FROM loyalty_voucher_codes
    WHERE tenant_id = NEW.tenant_id AND id = NEW.voucher_code_id
      AND status = 'available' AND expires_at > now()
    FOR UPDATE;
  IF NOT FOUND OR NEW.points_spent <> code_cost THEN
    RAISE EXCEPTION 'Voucher code is unavailable or points cost differs'
      USING ERRCODE = '23514';
  END IF;
  UPDATE loyalty_voucher_codes SET status = 'issued'
    WHERE tenant_id = NEW.tenant_id AND id = NEW.voucher_code_id;
  INSERT INTO loyalty_point_ledger
    (tenant_id, customer_id, voucher_issuance_id, reason, points_delta)
  VALUES (NEW.tenant_id, NEW.customer_id, NEW.id, 'redeem', -NEW.points_spent);
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_issue_voucher
  AFTER INSERT ON loyalty_voucher_issuances
  FOR EACH ROW EXECUTE FUNCTION loyalty_issue_voucher();

CREATE FUNCTION loyalty_check_point_entry() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  source_customer_id bigint;
  source_points bigint;
BEGIN
  IF NEW.claim_id IS NOT NULL THEN
    SELECT customer_id, points_awarded INTO source_customer_id, source_points
      FROM loyalty_claims
      WHERE tenant_id = NEW.tenant_id AND id = NEW.claim_id;
    IF NOT FOUND OR NEW.customer_id <> source_customer_id
        OR abs(NEW.points_delta) <> source_points THEN
      RAISE EXCEPTION 'Point entry does not match claim'
        USING ERRCODE = '23514';
    END IF;
  ELSE
    SELECT customer_id, points_spent INTO source_customer_id, source_points
      FROM loyalty_voucher_issuances
      WHERE tenant_id = NEW.tenant_id AND id = NEW.voucher_issuance_id;
    IF NOT FOUND OR NEW.customer_id <> source_customer_id
        OR -NEW.points_delta <> source_points THEN
      RAISE EXCEPTION 'Point entry does not match voucher issuance'
        USING ERRCODE = '23514';
    END IF;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_loyalty_check_point_entry
  BEFORE INSERT ON loyalty_point_ledger
  FOR EACH ROW EXECUTE FUNCTION loyalty_check_point_entry();

-- Tenant isolation matches the rest of the shared database. Customer-facing
-- authorization within one tenant must also be enforced by the application.
DO $$
DECLARE
  table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY[
    'customer_accounts', 'loyalty_claims', 'loyalty_point_accounts',
    'loyalty_voucher_codes', 'loyalty_voucher_issuances', 'loyalty_point_ledger'
  ] LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
    EXECUTE format(
      'CREATE POLICY tenant_isolation ON %I USING (tenant_id = app_current_tenant_id() OR app_is_super_admin()) WITH CHECK (tenant_id = app_current_tenant_id() OR app_is_super_admin())',
      table_name
    );
  END LOOP;
END $$;

GRANT SELECT, INSERT, UPDATE ON customer_accounts TO smartomni_app;
GRANT SELECT, INSERT ON loyalty_claims TO smartomni_app;
GRANT SELECT ON loyalty_point_accounts TO smartomni_app;
GRANT SELECT, INSERT ON loyalty_voucher_codes TO smartomni_app;
GRANT UPDATE (status, expires_at) ON loyalty_voucher_codes TO smartomni_app;
GRANT SELECT, INSERT ON loyalty_voucher_issuances TO smartomni_app;
GRANT SELECT, INSERT ON loyalty_point_ledger TO smartomni_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO smartomni_app;

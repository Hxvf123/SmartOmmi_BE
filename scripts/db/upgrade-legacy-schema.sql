-- Reviewed upgrade ONLY for the original 36-table Database/files schema.
-- Run as the existing database administrator after backup and stopping all services.
-- Abort on unexpected schema or nullable tenant data; never mark an unverified schema as baseline.
BEGIN;
DO $$
DECLARE t text;
BEGIN
  IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN
    RAISE EXCEPTION 'Database already has Flyway history; use a new versioned migration instead';
  END IF;
  FOREACH t IN ARRAY ARRAY['tenants','subscription_plans','plan_feature_flags','tenant_subscription_history','resource_usage','tenant_invoices','subscription_payments','users','password_reset_tokens','auth_sessions','marketplace_connections','products','product_skus','product_images','product_marketplace_links','price_sync_configs','price_sync_history','inventory','inventory_outbox_events','orders','order_items','order_status_history','returns_refunds','webhook_events_log','seller_wallet_ledger','promotion_events','promotion_items','ai_model_versions','demand_forecasts','sales_outlier_exclusions','user_item_interactions','product_recommendations_cache','redirect_click_events','support_tickets','security_audit_logs','system_metric_alerts'] LOOP
    IF to_regclass('public.' || t) IS NULL THEN
      RAISE EXCEPTION 'Missing expected legacy table: %', t;
    END IF;
  END LOOP;
  IF NOT EXISTS (SELECT FROM pg_type WHERE typname='user_role' AND typtype='e'
                 AND typnamespace='public'::regnamespace) THEN
    RAISE EXCEPTION 'Expected PostgreSQL enum schema; Hibernate-created schemas require a separate reconciliation';
  END IF;
END $$;

-- Save FK definitions before changing identifier types; restores the exact existing constraints.
CREATE TEMP TABLE legacy_foreign_keys ON COMMIT DROP AS
SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace;
DO $$
DECLARE c record;
BEGIN
  FOR c IN SELECT * FROM legacy_foreign_keys LOOP
    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', c.table_name, c.conname);
  END LOOP;
  FOR c IN SELECT tablename, policyname FROM pg_policies
           WHERE schemaname='public' AND tablename = ANY(ARRAY['tenants','subscription_plans','plan_feature_flags','tenant_subscription_history','resource_usage','tenant_invoices','subscription_payments','users','password_reset_tokens','auth_sessions','marketplace_connections','products','product_skus','product_images','product_marketplace_links','price_sync_configs','price_sync_history','inventory','inventory_outbox_events','orders','order_items','order_status_history','returns_refunds','webhook_events_log','seller_wallet_ledger','promotion_events','promotion_items','ai_model_versions','demand_forecasts','sales_outlier_exclusions','user_item_interactions','product_recommendations_cache','redirect_click_events','support_tickets','security_audit_logs','system_metric_alerts']) LOOP
    EXECUTE format('DROP POLICY %I ON public.%I', c.policyname, c.tablename);
  END LOOP;
END $$;
ALTER TABLE public.tenants ALTER COLUMN id TYPE bigint;
ALTER TABLE public.tenants ALTER COLUMN plan_id TYPE bigint;
ALTER TABLE public.tenants ALTER COLUMN locked_by TYPE bigint;
ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.tenants OWNER TO smartomni_migrator;
ALTER TABLE public.subscription_plans ALTER COLUMN id TYPE bigint;
ALTER TABLE public.subscription_plans ALTER COLUMN updated_by TYPE bigint;
ALTER TABLE public.subscription_plans ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.subscription_plans ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.subscription_plans OWNER TO smartomni_migrator;
ALTER TABLE public.plan_feature_flags ALTER COLUMN id TYPE bigint;
ALTER TABLE public.plan_feature_flags ALTER COLUMN plan_id TYPE bigint;
ALTER TABLE public.plan_feature_flags ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.plan_feature_flags ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.plan_feature_flags OWNER TO smartomni_migrator;
ALTER TABLE public.tenant_subscription_history ALTER COLUMN id TYPE bigint;
ALTER TABLE public.tenant_subscription_history ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.tenant_subscription_history ALTER COLUMN old_plan_id TYPE bigint;
ALTER TABLE public.tenant_subscription_history ALTER COLUMN new_plan_id TYPE bigint;
ALTER TABLE public.tenant_subscription_history ALTER COLUMN changed_by TYPE bigint;
ALTER TABLE public.tenant_subscription_history ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.tenant_subscription_history ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.tenant_subscription_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.tenant_subscription_history OWNER TO smartomni_migrator;
ALTER TABLE public.resource_usage ALTER COLUMN id TYPE bigint;
ALTER TABLE public.resource_usage ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.resource_usage ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.resource_usage ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.resource_usage ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.resource_usage OWNER TO smartomni_migrator;
ALTER TABLE public.tenant_invoices ALTER COLUMN id TYPE bigint;
ALTER TABLE public.tenant_invoices ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.tenant_invoices ALTER COLUMN plan_id TYPE bigint;
ALTER TABLE public.tenant_invoices ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.tenant_invoices ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.tenant_invoices ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.tenant_invoices OWNER TO smartomni_migrator;
ALTER TABLE public.subscription_payments ALTER COLUMN id TYPE bigint;
ALTER TABLE public.subscription_payments ALTER COLUMN invoice_id TYPE bigint;
ALTER TABLE public.subscription_payments ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.subscription_payments ALTER COLUMN confirmed_by TYPE bigint;
ALTER TABLE public.subscription_payments ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.subscription_payments ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.subscription_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.subscription_payments OWNER TO smartomni_migrator;
ALTER TABLE public.users ALTER COLUMN id TYPE bigint;
ALTER TABLE public.users ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.users OWNER TO smartomni_migrator;
ALTER TABLE public.password_reset_tokens ALTER COLUMN id TYPE bigint;
ALTER TABLE public.password_reset_tokens ALTER COLUMN user_id TYPE bigint;
ALTER TABLE public.password_reset_tokens ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.password_reset_tokens ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.password_reset_tokens OWNER TO smartomni_migrator;
ALTER TABLE public.auth_sessions ALTER COLUMN id TYPE bigint;
ALTER TABLE public.auth_sessions ALTER COLUMN user_id TYPE bigint;
ALTER TABLE public.auth_sessions ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.auth_sessions ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.auth_sessions OWNER TO smartomni_migrator;
ALTER TABLE public.marketplace_connections ALTER COLUMN id TYPE bigint;
ALTER TABLE public.marketplace_connections ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.marketplace_connections ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.marketplace_connections ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.marketplace_connections ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.marketplace_connections OWNER TO smartomni_migrator;
ALTER TABLE public.products ALTER COLUMN id TYPE bigint;
ALTER TABLE public.products ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.products ALTER COLUMN created_by TYPE bigint;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.products ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.products OWNER TO smartomni_migrator;
ALTER TABLE public.product_skus ALTER COLUMN id TYPE bigint;
ALTER TABLE public.product_skus ALTER COLUMN product_id TYPE bigint;
ALTER TABLE public.product_skus ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.product_skus ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.product_skus ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.product_skus ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.product_skus OWNER TO smartomni_migrator;
ALTER TABLE public.product_images ALTER COLUMN id TYPE bigint;
ALTER TABLE public.product_images ALTER COLUMN product_id TYPE bigint;
ALTER TABLE public.product_images ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.product_images ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.product_images ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.product_images ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.product_images ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.product_images OWNER TO smartomni_migrator;
ALTER TABLE public.product_marketplace_links ALTER COLUMN id TYPE bigint;
ALTER TABLE public.product_marketplace_links ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.product_marketplace_links ALTER COLUMN connection_id TYPE bigint;
ALTER TABLE public.product_marketplace_links ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.product_marketplace_links ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.product_marketplace_links ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.product_marketplace_links ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.product_marketplace_links OWNER TO smartomni_migrator;
ALTER TABLE public.price_sync_configs ALTER COLUMN id TYPE bigint;
ALTER TABLE public.price_sync_configs ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.price_sync_configs ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.price_sync_configs ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.price_sync_configs ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.price_sync_configs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.price_sync_configs OWNER TO smartomni_migrator;
ALTER TABLE public.price_sync_history ALTER COLUMN id TYPE bigint;
ALTER TABLE public.price_sync_history ALTER COLUMN sync_config_id TYPE bigint;
ALTER TABLE public.price_sync_history ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.price_sync_history ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.price_sync_history ALTER COLUMN confirmed_by TYPE bigint;
ALTER TABLE public.price_sync_history ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.price_sync_history ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.price_sync_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.price_sync_history OWNER TO smartomni_migrator;
ALTER TABLE public.inventory ALTER COLUMN id TYPE bigint;
ALTER TABLE public.inventory ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.inventory ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.inventory ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.inventory OWNER TO smartomni_migrator;
ALTER TABLE public.inventory_outbox_events ALTER COLUMN id TYPE bigint;
ALTER TABLE public.inventory_outbox_events ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.inventory_outbox_events ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.inventory_outbox_events ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.inventory_outbox_events ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.inventory_outbox_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.inventory_outbox_events OWNER TO smartomni_migrator;
ALTER TABLE public.orders ALTER COLUMN id TYPE bigint;
ALTER TABLE public.orders ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.orders ALTER COLUMN connection_id TYPE bigint;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.orders ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.orders OWNER TO smartomni_migrator;
ALTER TABLE public.order_items ALTER COLUMN id TYPE bigint;
ALTER TABLE public.order_items ALTER COLUMN order_id TYPE bigint;
ALTER TABLE public.order_items ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.order_items ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.order_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.order_items OWNER TO smartomni_migrator;
ALTER TABLE public.order_status_history ALTER COLUMN id TYPE bigint;
ALTER TABLE public.order_status_history ALTER COLUMN order_id TYPE bigint;
ALTER TABLE public.order_status_history ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.order_status_history ALTER COLUMN changed_by TYPE bigint;
ALTER TABLE public.order_status_history ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.order_status_history ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.order_status_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.order_status_history OWNER TO smartomni_migrator;
ALTER TABLE public.returns_refunds ALTER COLUMN id TYPE bigint;
ALTER TABLE public.returns_refunds ALTER COLUMN order_id TYPE bigint;
ALTER TABLE public.returns_refunds ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.returns_refunds ALTER COLUMN reviewed_by TYPE bigint;
ALTER TABLE public.returns_refunds ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.returns_refunds ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.returns_refunds ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.returns_refunds OWNER TO smartomni_migrator;
ALTER TABLE public.webhook_events_log ALTER COLUMN id TYPE bigint;
ALTER TABLE public.webhook_events_log ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.webhook_events_log ALTER COLUMN order_id TYPE bigint;
ALTER TABLE public.webhook_events_log ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.webhook_events_log ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.webhook_events_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.webhook_events_log OWNER TO smartomni_migrator;
ALTER TABLE public.seller_wallet_ledger ALTER COLUMN id TYPE bigint;
ALTER TABLE public.seller_wallet_ledger ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.seller_wallet_ledger ALTER COLUMN order_id TYPE bigint;
ALTER TABLE public.seller_wallet_ledger ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.seller_wallet_ledger ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.seller_wallet_ledger ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.seller_wallet_ledger OWNER TO smartomni_migrator;
ALTER TABLE public.promotion_events ALTER COLUMN id TYPE bigint;
ALTER TABLE public.promotion_events ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.promotion_events ALTER COLUMN created_by TYPE bigint;
ALTER TABLE public.promotion_events ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.promotion_events ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.promotion_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.promotion_events OWNER TO smartomni_migrator;
ALTER TABLE public.promotion_items ALTER COLUMN id TYPE bigint;
ALTER TABLE public.promotion_items ALTER COLUMN promotion_id TYPE bigint;
ALTER TABLE public.promotion_items ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.promotion_items ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.promotion_items ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.promotion_items OWNER TO smartomni_migrator;
ALTER TABLE public.ai_model_versions ALTER COLUMN id TYPE bigint;
ALTER TABLE public.ai_model_versions ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.ai_model_versions ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.ai_model_versions ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.ai_model_versions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.ai_model_versions OWNER TO smartomni_migrator;
ALTER TABLE public.demand_forecasts ALTER COLUMN id TYPE bigint;
ALTER TABLE public.demand_forecasts ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.demand_forecasts ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.demand_forecasts ALTER COLUMN model_version_id TYPE bigint;
ALTER TABLE public.demand_forecasts ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.demand_forecasts ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.demand_forecasts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.demand_forecasts OWNER TO smartomni_migrator;
ALTER TABLE public.sales_outlier_exclusions ALTER COLUMN id TYPE bigint;
ALTER TABLE public.sales_outlier_exclusions ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.sales_outlier_exclusions ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.sales_outlier_exclusions ALTER COLUMN order_id TYPE bigint;
ALTER TABLE public.sales_outlier_exclusions ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.sales_outlier_exclusions ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.sales_outlier_exclusions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.sales_outlier_exclusions OWNER TO smartomni_migrator;
ALTER TABLE public.user_item_interactions ALTER COLUMN id TYPE bigint;
ALTER TABLE public.user_item_interactions ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.user_item_interactions ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.user_item_interactions ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.user_item_interactions ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.user_item_interactions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.user_item_interactions OWNER TO smartomni_migrator;
ALTER TABLE public.product_recommendations_cache ALTER COLUMN id TYPE bigint;
ALTER TABLE public.product_recommendations_cache ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.product_recommendations_cache ALTER COLUMN source_sku_id TYPE bigint;
ALTER TABLE public.product_recommendations_cache ALTER COLUMN recommended_sku_id TYPE bigint;
ALTER TABLE public.product_recommendations_cache ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.product_recommendations_cache ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.product_recommendations_cache ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.product_recommendations_cache OWNER TO smartomni_migrator;
ALTER TABLE public.redirect_click_events ALTER COLUMN id TYPE bigint;
ALTER TABLE public.redirect_click_events ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.redirect_click_events ALTER COLUMN sku_id TYPE bigint;
ALTER TABLE public.redirect_click_events ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.redirect_click_events ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.redirect_click_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.redirect_click_events OWNER TO smartomni_migrator;
ALTER TABLE public.support_tickets ALTER COLUMN id TYPE bigint;
ALTER TABLE public.support_tickets ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.support_tickets ALTER COLUMN created_by TYPE bigint;
ALTER TABLE public.support_tickets ALTER COLUMN assigned_to TYPE bigint;
ALTER TABLE public.support_tickets ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.support_tickets ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.support_tickets ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.support_tickets OWNER TO smartomni_migrator;
ALTER TABLE public.security_audit_logs ALTER COLUMN id TYPE bigint;
ALTER TABLE public.security_audit_logs ALTER COLUMN tenant_id TYPE bigint;
ALTER TABLE public.security_audit_logs ALTER COLUMN session_tenant_id TYPE bigint;
ALTER TABLE public.security_audit_logs ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.security_audit_logs ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.security_audit_logs OWNER TO smartomni_migrator;
ALTER TABLE public.system_metric_alerts ALTER COLUMN id TYPE bigint;
ALTER TABLE public.system_metric_alerts ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
ALTER TABLE public.system_metric_alerts ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE public.system_metric_alerts OWNER TO smartomni_migrator;
DO $$
DECLARE c record;
BEGIN
  FOR c IN SELECT * FROM legacy_foreign_keys LOOP
    EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', c.table_name, c.conname, c.definition);
  END LOOP;
  FOR c IN SELECT schemaname, sequencename FROM pg_sequences WHERE schemaname='public' LOOP
    EXECUTE format('ALTER SEQUENCE %I.%I AS bigint', c.schemaname, c.sequencename);
  END LOOP;
END $$;
DROP FUNCTION app_current_tenant_id();
ALTER TYPE public.user_role OWNER TO smartomni_migrator;
ALTER TYPE public.user_status OWNER TO smartomni_migrator;
ALTER TYPE public.tenant_status OWNER TO smartomni_migrator;
ALTER TYPE public.platform_type OWNER TO smartomni_migrator;
ALTER TYPE public.connection_status OWNER TO smartomni_migrator;
ALTER TYPE public.product_status OWNER TO smartomni_migrator;
ALTER TYPE public.order_status OWNER TO smartomni_migrator;
ALTER TYPE public.order_change_source OWNER TO smartomni_migrator;
ALTER TYPE public.order_source OWNER TO smartomni_migrator;
ALTER TYPE public.return_trigger_type OWNER TO smartomni_migrator;
ALTER TYPE public.refund_status OWNER TO smartomni_migrator;
ALTER TYPE public.inspection_result_type OWNER TO smartomni_migrator;
ALTER TYPE public.dispute_status_type OWNER TO smartomni_migrator;
ALTER TYPE public.outbox_status OWNER TO smartomni_migrator;
ALTER TYPE public.promotion_scope OWNER TO smartomni_migrator;
ALTER TYPE public.model_type OWNER TO smartomni_migrator;
ALTER TYPE public.model_status OWNER TO smartomni_migrator;
ALTER TYPE public.interaction_type OWNER TO smartomni_migrator;
ALTER TYPE public.ticket_status OWNER TO smartomni_migrator;
ALTER TYPE public.security_event_type OWNER TO smartomni_migrator;
ALTER TYPE public.sync_scope OWNER TO smartomni_migrator;
ALTER TYPE public.billing_interval OWNER TO smartomni_migrator;
ALTER TYPE public.invoice_status OWNER TO smartomni_migrator;
ALTER TYPE public.payment_status OWNER TO smartomni_migrator;
-- Application roles must be provisioned before services start.
GRANT USAGE ON SCHEMA public TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE tenants TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE subscription_plans TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE plan_feature_flags TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE tenant_subscription_history TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE resource_usage TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE tenant_invoices TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE subscription_payments TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE users TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE password_reset_tokens TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE auth_sessions TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE marketplace_connections TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE products TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_skus TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_images TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_marketplace_links TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE price_sync_configs TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE price_sync_history TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE inventory TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE inventory_outbox_events TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE orders TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE order_items TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE order_status_history TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE returns_refunds TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE webhook_events_log TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE seller_wallet_ledger TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE promotion_events TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE promotion_items TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE ai_model_versions TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE demand_forecasts TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE sales_outlier_exclusions TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_item_interactions TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_recommendations_cache TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE redirect_click_events TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE support_tickets TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE security_audit_logs TO smartomni_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE system_metric_alerts TO smartomni_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO smartomni_app;
-- ---------------------------------------------------------
-- 2) Helper functions đọc session context
--    Backend PHẢI set 2 biến này ngay khi mở transaction/connection:
--      SET LOCAL app.tenant_id = '<tenant_id>';   -- rỗng '' nếu super_admin
--      SET LOCAL app.user_role = '<role>';        -- super_admin | admin | manager
--    Dùng SET LOCAL (không phải SET) để tự động reset khi transaction
--    kết thúc, tránh rò rỉ context giữa các request dùng chung
--    connection pool (PgBouncer transaction-mode vẫn an toàn).
-- ---------------------------------------------------------
CREATE OR REPLACE FUNCTION app_current_tenant_id()
RETURNS bigint
LANGUAGE sql
STABLE
AS $$
  SELECT NULLIF(current_setting('app.tenant_id', true), '')::bigint
$$;

CREATE OR REPLACE FUNCTION app_current_role()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT NULLIF(current_setting('app.user_role', true), '')
$$;

CREATE OR REPLACE FUNCTION app_is_super_admin()
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
  SELECT current_setting('app.user_role', true) = 'super_admin'
$$;

COMMENT ON FUNCTION app_current_tenant_id() IS 'Đọc tenant_id hiện tại từ session GUC app.tenant_id do backend set qua SET LOCAL sau khi verify JWT.';
COMMENT ON FUNCTION app_is_super_admin() IS 'True nếu session hiện tại thuộc super_admin (bypass tenant isolation theo policy).';
ALTER FUNCTION public.app_current_tenant_id() OWNER TO smartomni_migrator;
ALTER FUNCTION public.app_current_role() OWNER TO smartomni_migrator;
ALTER FUNCTION public.app_is_super_admin() OWNER TO smartomni_migrator;
-- =========================================================
-- SMARTOMNI - 05_rls_policies.sql
-- Bật RLS + tạo policy cho toàn bộ 36 bảng.
-- Chạy sau 04_roles_and_functions.sql
-- =========================================================

-- =========================================================
-- NHÓM A: 27 bảng có cột tenant_id trực tiếp, not null,
-- dùng chung 1 policy pattern -> tạo bằng vòng lặp cho gọn.
-- =========================================================
DO $$
DECLARE
  t text;
  tenant_tables text[] := ARRAY[
    'tenant_subscription_history',
    'resource_usage',
    'tenant_invoices',
    'subscription_payments',
    'marketplace_connections',
    'products',
    'product_skus',
    'product_images',
    'product_marketplace_links',
    'price_sync_configs',
    'price_sync_history',
    'inventory',
    'inventory_outbox_events',
    'orders',
    'order_items',
    'order_status_history',
    'returns_refunds',
    'webhook_events_log',
    'seller_wallet_ledger',
    'promotion_events',
    'ai_model_versions',
    'demand_forecasts',
    'sales_outlier_exclusions',
    'user_item_interactions',
    'product_recommendations_cache',
    'redirect_click_events',
    'support_tickets'
  ];
BEGIN
  FOREACH t IN ARRAY tenant_tables LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
    EXECUTE format(
      $f$CREATE POLICY tenant_isolation ON %I
         USING (tenant_id = app_current_tenant_id() OR app_is_super_admin())
         WITH CHECK (tenant_id = app_current_tenant_id() OR app_is_super_admin())$f$,
      t
    );
  END LOOP;
END $$;

-- =========================================================
-- NHÓM B: Bảng gốc "tenants" - policy theo chính id
-- =========================================================
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenants FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tenants
  USING (id = app_current_tenant_id() OR app_is_super_admin())
  WITH CHECK (id = app_current_tenant_id() OR app_is_super_admin());
-- Lưu ý: tạo tenant mới (INSERT) chỉ super_admin làm được theo policy này,
-- vì app_current_tenant_id() lúc đó chưa có giá trị khớp id mới sinh ra.
-- Đây là hành vi ĐÚNG với nghiệp vụ: chỉ Super Admin được tạo tenant.

-- =========================================================
-- NHÓM C: "users" - tenant_id NULLABLE (NULL khi role=super_admin)
-- =========================================================
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON users
  USING (app_is_super_admin() OR tenant_id = app_current_tenant_id())
  WITH CHECK (app_is_super_admin() OR tenant_id = app_current_tenant_id());

-- =========================================================
-- NHÓM D: Bảng KHÔNG có tenant_id trực tiếp -> policy join
-- =========================================================

-- password_reset_tokens (join qua users)
ALTER TABLE password_reset_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_tokens FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON password_reset_tokens
  USING (
    app_is_super_admin()
    OR EXISTS (
      SELECT 1 FROM users u
      WHERE u.id = password_reset_tokens.user_id
        AND u.tenant_id = app_current_tenant_id()
    )
  )
  WITH CHECK (
    app_is_super_admin()
    OR EXISTS (
      SELECT 1 FROM users u
      WHERE u.id = password_reset_tokens.user_id
        AND u.tenant_id = app_current_tenant_id()
    )
  );

-- auth_sessions (join qua users)
ALTER TABLE auth_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE auth_sessions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON auth_sessions
  USING (
    app_is_super_admin()
    OR EXISTS (
      SELECT 1 FROM users u
      WHERE u.id = auth_sessions.user_id
        AND u.tenant_id = app_current_tenant_id()
    )
  )
  WITH CHECK (
    app_is_super_admin()
    OR EXISTS (
      SELECT 1 FROM users u
      WHERE u.id = auth_sessions.user_id
        AND u.tenant_id = app_current_tenant_id()
    )
  );

-- promotion_items (join qua promotion_events)
ALTER TABLE promotion_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_items FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON promotion_items
  USING (
    app_is_super_admin()
    OR EXISTS (
      SELECT 1 FROM promotion_events pe
      WHERE pe.id = promotion_items.promotion_id
        AND pe.tenant_id = app_current_tenant_id()
    )
  )
  WITH CHECK (
    app_is_super_admin()
    OR EXISTS (
      SELECT 1 FROM promotion_events pe
      WHERE pe.id = promotion_items.promotion_id
        AND pe.tenant_id = app_current_tenant_id()
    )
  );

-- =========================================================
-- NHÓM E: Dữ liệu GLOBAL (không thuộc tenant) - đọc chung,
-- ghi chỉ super_admin
-- =========================================================

ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_plans FORCE ROW LEVEL SECURITY;

CREATE POLICY plans_select_all ON subscription_plans
  FOR SELECT USING (true);
CREATE POLICY plans_insert_super_admin ON subscription_plans
  FOR INSERT WITH CHECK (app_is_super_admin());
CREATE POLICY plans_update_super_admin ON subscription_plans
  FOR UPDATE USING (app_is_super_admin()) WITH CHECK (app_is_super_admin());
CREATE POLICY plans_delete_super_admin ON subscription_plans
  FOR DELETE USING (app_is_super_admin());

ALTER TABLE plan_feature_flags ENABLE ROW LEVEL SECURITY;
ALTER TABLE plan_feature_flags FORCE ROW LEVEL SECURITY;

CREATE POLICY flags_select_all ON plan_feature_flags
  FOR SELECT USING (true);
CREATE POLICY flags_insert_super_admin ON plan_feature_flags
  FOR INSERT WITH CHECK (app_is_super_admin());
CREATE POLICY flags_update_super_admin ON plan_feature_flags
  FOR UPDATE USING (app_is_super_admin()) WITH CHECK (app_is_super_admin());
CREATE POLICY flags_delete_super_admin ON plan_feature_flags
  FOR DELETE USING (app_is_super_admin());

-- =========================================================
-- NHÓM F: security_audit_logs - tenant_id nullable
-- (super_admin xem tất cả kể cả sự kiện hệ thống tenant_id NULL)
-- =========================================================
ALTER TABLE security_audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_audit_logs FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON security_audit_logs
  USING (app_is_super_admin() OR tenant_id = app_current_tenant_id())
  WITH CHECK (app_is_super_admin() OR tenant_id = app_current_tenant_id());

-- =========================================================
-- NHÓM G: system_metric_alerts - chỉ super_admin, không tenant
-- =========================================================
ALTER TABLE system_metric_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE system_metric_alerts FORCE ROW LEVEL SECURITY;

CREATE POLICY super_admin_only ON system_metric_alerts
  USING (app_is_super_admin())
  WITH CHECK (app_is_super_admin());

-- =========================================================
-- Kiểm tra nhanh: liệt kê bảng nào CHƯA bật RLS (phải trả về 0 dòng)
-- =========================================================
-- SELECT relname FROM pg_class c
-- JOIN pg_namespace n ON n.oid = c.relnamespace
-- WHERE n.nspname = 'public' AND c.relkind = 'r' AND NOT c.relrowsecurity;

COMMIT;
-- Now verify schema/data, explicitly baseline at V5, then start services to apply V6.

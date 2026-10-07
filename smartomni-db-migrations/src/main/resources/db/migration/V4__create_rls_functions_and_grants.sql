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

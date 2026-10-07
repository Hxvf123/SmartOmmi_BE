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

-- =========================================================
-- SMARTOMNI - V18__seed_subscription_plans_and_public_tenant_rls.sql
-- 1. Seed default subscription_plans (FREE, PRO) and plan_feature_flags
--    idempotently so onboarding works on a fresh database (B7).
-- 2. Allow public SELECT on active tenants under RLS so unauthenticated
--    storefront lookup (/api/v1/tenants/public/by-subdomain/{subdomain})
--    succeeds without requiring a JWT/tenant session (B3).
-- =========================================================

SET LOCAL app.user_role = 'super_admin';

INSERT INTO subscription_plans (
    name,
    price,
    billing_interval,
    max_skus,
    max_marketplace_connections,
    storage_limit_mb
)
VALUES
    ('FREE', 0.00, 'monthly', 100, 1, 1024),
    ('PRO', 499000.00, 'monthly', 2147483647, 2147483647, 10240)
ON CONFLICT (name) DO NOTHING;

INSERT INTO plan_feature_flags (plan_id, feature_key, enabled)
SELECT sp.id, 'ai_features', CASE WHEN sp.name = 'PRO' THEN true ELSE false END
FROM subscription_plans sp
WHERE sp.name IN ('FREE', 'PRO')
ON CONFLICT (plan_id, feature_key) DO NOTHING;

DROP POLICY IF EXISTS tenants_public_select_active ON tenants;
CREATE POLICY tenants_public_select_active ON tenants
  FOR SELECT
  USING (status = 'active');


-- ============================================================================
-- V9: Bổ sung các giá trị enum mới cho user_role: 'tenant_admin', 'customer'
-- ============================================================================

ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'tenant_admin';
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'customer';

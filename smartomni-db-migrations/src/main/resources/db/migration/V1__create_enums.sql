-- =========================================================
-- SMARTOMNI - 01_enums.sql
-- Định nghĩa toàn bộ ENUM types
-- Chạy với: psql -d smartomni -f 01_enums.sql
-- =========================================================

CREATE TYPE user_role AS ENUM ('super_admin', 'admin', 'manager');
CREATE TYPE user_status AS ENUM ('active', 'disabled');
CREATE TYPE tenant_status AS ENUM ('active', 'locked');
CREATE TYPE platform_type AS ENUM ('shopee', 'tiktok_shop', 'storefront');
CREATE TYPE connection_status AS ENUM ('connected', 'error', 'disconnected');
CREATE TYPE product_status AS ENUM ('active', 'hidden');
CREATE TYPE order_status AS ENUM (
  'pending', 'to_ship', 'shipped', 'delivering', 'completed', 'cancelled', 'returned'
);
CREATE TYPE order_change_source AS ENUM ('manager', 'marketplace_webhook', 'system');
CREATE TYPE order_source AS ENUM ('webhook', 'polling', 'manual');
CREATE TYPE return_trigger_type AS ENUM ('refused_delivery', 'customer_request');
CREATE TYPE refund_status AS ENUM ('requested', 'approved', 'rejected', 'completed');
CREATE TYPE inspection_result_type AS ENUM (
  'pending', 'matched', 'damaged', 'swapped', 'missing_items'
);
CREATE TYPE dispute_status_type AS ENUM ('none', 'filed', 'approved', 'denied');
CREATE TYPE outbox_status AS ENUM ('pending', 'sent', 'failed');
CREATE TYPE promotion_scope AS ENUM ('all', 'category', 'sku');
CREATE TYPE model_type AS ENUM ('demand_forecasting', 'recommendation');
CREATE TYPE model_status AS ENUM ('training', 'active', 'rolled_back');
CREATE TYPE interaction_type AS ENUM ('view', 'cart', 'purchase');
CREATE TYPE ticket_status AS ENUM ('open', 'in_progress', 'resolved');
CREATE TYPE security_event_type AS ENUM (
  'rls_violation', 'cross_tenant_test', 'login_failure', 'brute_force_lock'
);
CREATE TYPE sync_scope AS ENUM ('tenant', 'product');
CREATE TYPE billing_interval AS ENUM ('monthly', 'yearly');
CREATE TYPE invoice_status AS ENUM ('pending', 'paid', 'overdue', 'cancelled');
CREATE TYPE payment_status AS ENUM ('pending', 'success', 'failed');

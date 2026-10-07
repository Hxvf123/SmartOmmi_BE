-- =========================================================
-- SMARTOMNI - 03_foreign_keys.sql
-- Thêm toàn bộ Foreign Key SAU KHI mọi bảng đã tồn tại
-- (tránh lỗi phụ thuộc vòng tenants <-> users)
-- Chạy sau 02_tables.sql
-- =========================================================

-- tenants
ALTER TABLE tenants
  ADD CONSTRAINT fk_tenants_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
  ADD CONSTRAINT fk_tenants_locked_by FOREIGN KEY (locked_by) REFERENCES users(id);

-- plan_feature_flags
ALTER TABLE plan_feature_flags
  ADD CONSTRAINT fk_plan_feature_flags_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id);

-- tenant_subscription_history
ALTER TABLE tenant_subscription_history
  ADD CONSTRAINT fk_tsh_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_tsh_old_plan FOREIGN KEY (old_plan_id) REFERENCES subscription_plans(id),
  ADD CONSTRAINT fk_tsh_new_plan FOREIGN KEY (new_plan_id) REFERENCES subscription_plans(id),
  ADD CONSTRAINT fk_tsh_changed_by FOREIGN KEY (changed_by) REFERENCES users(id);

-- resource_usage (1-1 với tenants)
ALTER TABLE resource_usage
  ADD CONSTRAINT fk_resource_usage_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- tenant_invoices
ALTER TABLE tenant_invoices
  ADD CONSTRAINT fk_invoices_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_invoices_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id);

-- subscription_payments
ALTER TABLE subscription_payments
  ADD CONSTRAINT fk_sub_payments_invoice FOREIGN KEY (invoice_id) REFERENCES tenant_invoices(id),
  ADD CONSTRAINT fk_sub_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_sub_payments_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(id);

-- users
ALTER TABLE users
  ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- password_reset_tokens
ALTER TABLE password_reset_tokens
  ADD CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id);

-- auth_sessions
ALTER TABLE auth_sessions
  ADD CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users(id);

-- marketplace_connections
ALTER TABLE marketplace_connections
  ADD CONSTRAINT fk_mc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- products
ALTER TABLE products
  ADD CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_products_created_by FOREIGN KEY (created_by) REFERENCES users(id);

-- product_skus
ALTER TABLE product_skus
  ADD CONSTRAINT fk_skus_product FOREIGN KEY (product_id) REFERENCES products(id),
  ADD CONSTRAINT fk_skus_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- product_images
ALTER TABLE product_images
  ADD CONSTRAINT fk_pimg_product FOREIGN KEY (product_id) REFERENCES products(id),
  ADD CONSTRAINT fk_pimg_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_pimg_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- product_marketplace_links
ALTER TABLE product_marketplace_links
  ADD CONSTRAINT fk_pml_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_pml_connection FOREIGN KEY (connection_id) REFERENCES marketplace_connections(id),
  ADD CONSTRAINT fk_pml_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- price_sync_configs
ALTER TABLE price_sync_configs
  ADD CONSTRAINT fk_psc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_psc_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id);

-- price_sync_history
ALTER TABLE price_sync_history
  ADD CONSTRAINT fk_psh_config FOREIGN KEY (sync_config_id) REFERENCES price_sync_configs(id),
  ADD CONSTRAINT fk_psh_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_psh_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_psh_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(id);

-- inventory (1-1 với product_skus)
ALTER TABLE inventory
  ADD CONSTRAINT fk_inventory_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_inventory_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- inventory_outbox_events
ALTER TABLE inventory_outbox_events
  ADD CONSTRAINT fk_ioe_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_ioe_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- orders
ALTER TABLE orders
  ADD CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_orders_connection FOREIGN KEY (connection_id) REFERENCES marketplace_connections(id);

-- order_items
ALTER TABLE order_items
  ADD CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders(id),
  ADD CONSTRAINT fk_oi_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_oi_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id);

-- order_status_history
ALTER TABLE order_status_history
  ADD CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders(id),
  ADD CONSTRAINT fk_osh_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_osh_changed_by FOREIGN KEY (changed_by) REFERENCES users(id);

-- returns_refunds
ALTER TABLE returns_refunds
  ADD CONSTRAINT fk_rr_order FOREIGN KEY (order_id) REFERENCES orders(id),
  ADD CONSTRAINT fk_rr_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_rr_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id);

-- webhook_events_log
ALTER TABLE webhook_events_log
  ADD CONSTRAINT fk_wel_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_wel_order FOREIGN KEY (order_id) REFERENCES orders(id);

-- seller_wallet_ledger
ALTER TABLE seller_wallet_ledger
  ADD CONSTRAINT fk_swl_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_swl_order FOREIGN KEY (order_id) REFERENCES orders(id);

-- promotion_events
ALTER TABLE promotion_events
  ADD CONSTRAINT fk_pe_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_pe_created_by FOREIGN KEY (created_by) REFERENCES users(id);

-- promotion_items
ALTER TABLE promotion_items
  ADD CONSTRAINT fk_pi_promotion FOREIGN KEY (promotion_id) REFERENCES promotion_events(id),
  ADD CONSTRAINT fk_pi_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id);

-- ai_model_versions
ALTER TABLE ai_model_versions
  ADD CONSTRAINT fk_amv_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- demand_forecasts
ALTER TABLE demand_forecasts
  ADD CONSTRAINT fk_df_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_df_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_df_model_version FOREIGN KEY (model_version_id) REFERENCES ai_model_versions(id);

-- sales_outlier_exclusions
ALTER TABLE sales_outlier_exclusions
  ADD CONSTRAINT fk_soe_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_soe_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_soe_order FOREIGN KEY (order_id) REFERENCES orders(id);

-- user_item_interactions
ALTER TABLE user_item_interactions
  ADD CONSTRAINT fk_uii_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_uii_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id);

-- product_recommendations_cache
ALTER TABLE product_recommendations_cache
  ADD CONSTRAINT fk_prc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_prc_source_sku FOREIGN KEY (source_sku_id) REFERENCES product_skus(id),
  ADD CONSTRAINT fk_prc_rec_sku FOREIGN KEY (recommended_sku_id) REFERENCES product_skus(id);

-- redirect_click_events
ALTER TABLE redirect_click_events
  ADD CONSTRAINT fk_rce_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_rce_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id);

-- support_tickets
ALTER TABLE support_tickets
  ADD CONSTRAINT fk_st_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  ADD CONSTRAINT fk_st_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  ADD CONSTRAINT fk_st_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id);

-- security_audit_logs
ALTER TABLE security_audit_logs
  ADD CONSTRAINT fk_sal_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- Index bổ sung hữu ích cho các cột FK/lookup thường dùng
CREATE INDEX idx_orders_tenant_status ON orders (tenant_id, status);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_products_tenant ON products (tenant_id);
CREATE INDEX idx_product_skus_product ON product_skus (product_id);
CREATE INDEX idx_users_tenant ON users (tenant_id);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX idx_auth_sessions_user ON auth_sessions (user_id);
CREATE INDEX idx_promotion_items_promotion ON promotion_items (promotion_id);

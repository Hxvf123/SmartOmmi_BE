-- =========================================================
-- SMARTOMNI - 02_tables.sql
-- Tạo bảng KHÔNG kèm Foreign Key (tránh lỗi phụ thuộc vòng
-- giữa tenants <-> users). FK sẽ được thêm ở 03_foreign_keys.sql
-- Chạy sau 01_enums.sql
-- =========================================================

-- ---------- 0. TENANT & SUBSCRIPTION ----------

CREATE TABLE tenants (
  id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name             varchar,
  subdomain        varchar NOT NULL,
  status           tenant_status DEFAULT 'active',
  plan_id          integer,
  billing_cycle    billing_interval DEFAULT 'monthly',
  next_billing_date date,
  auto_renew       boolean DEFAULT true,
  locked_reason    varchar,
  locked_by        integer,
  locked_at        timestamptz,
  created_at       timestamptz DEFAULT now(),
  updated_at       timestamptz,
  CONSTRAINT uq_tenants_subdomain UNIQUE (subdomain)
);
COMMENT ON TABLE tenants IS 'Mọi bảng nghiệp vụ có tenant_id đều bật Row-Level Security (RLS) tham chiếu tới bảng này.';

CREATE TABLE subscription_plans (
  id                           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name                         varchar,
  price                        numeric(14,2),
  billing_interval             billing_interval DEFAULT 'monthly',
  max_skus                     integer,
  max_marketplace_connections  integer,
  storage_limit_mb             integer,
  updated_by                   integer,
  created_at                   timestamptz DEFAULT now(),
  updated_at                   timestamptz,
  CONSTRAINT uq_subscription_plans_name UNIQUE (name)
);
COMMENT ON TABLE subscription_plans IS 'Định nghĩa gói dịch vụ - Super Admin quản lý. Dữ liệu GLOBAL, không thuộc riêng tenant nào.';

CREATE TABLE plan_feature_flags (
  id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  plan_id      integer,
  feature_key  varchar,
  enabled      boolean DEFAULT false,
  CONSTRAINT uq_plan_feature_flags UNIQUE (plan_id, feature_key)
);
COMMENT ON TABLE plan_feature_flags IS 'Feature flag chi tiết theo từng gói. Dữ liệu GLOBAL.';

CREATE TABLE tenant_subscription_history (
  id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id    integer,
  old_plan_id  integer,
  new_plan_id  integer,
  changed_by   integer,
  changed_at   timestamptz DEFAULT now()
);
COMMENT ON TABLE tenant_subscription_history IS 'Lịch sử nâng/hạ cấp gói dịch vụ (Luồng 11).';

CREATE TABLE resource_usage (
  id                              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id                       integer,
  sku_count                       integer DEFAULT 0,
  marketplace_connections_count   integer DEFAULT 0,
  storage_used_mb                 integer DEFAULT 0,
  last_calculated_at              timestamptz,
  CONSTRAINT uq_resource_usage_tenant UNIQUE (tenant_id)
);
COMMENT ON TABLE resource_usage IS 'Theo dõi mức dùng tài nguyên so với giới hạn gói - dùng để chặn Downgrade (Luồng 11). Quan hệ 1-1 với tenants.';

-- ---------- 0B. BILLING ----------

CREATE TABLE tenant_invoices (
  id                    integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id             integer,
  plan_id               integer,
  billing_period_start  date,
  billing_period_end    date,
  amount                numeric(14,2),
  status                invoice_status DEFAULT 'pending',
  due_date              date,
  created_at            timestamptz DEFAULT now()
);
COMMENT ON TABLE tenant_invoices IS 'Hóa đơn theo từng chu kỳ billing của tenant.';

CREATE TABLE subscription_payments (
  id                       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  invoice_id               integer,
  tenant_id                integer,
  payment_method           varchar,
  gateway_transaction_id   varchar,
  amount                   numeric(14,2),
  signature_valid          boolean,
  is_anomaly               boolean DEFAULT false,
  confirmed_by             integer,
  status                   payment_status DEFAULT 'pending',
  paid_at                  timestamptz,
  created_at               timestamptz DEFAULT now(),
  CONSTRAINT uq_subscription_payments_gw_txn UNIQUE (gateway_transaction_id)
);
COMMENT ON TABLE subscription_payments IS 'Giao dịch thanh toán NÂNG CẤP GÓI DỊCH VỤ. KHÔNG dùng cho đơn hàng khách mua.';
COMMENT ON COLUMN subscription_payments.signature_valid IS 'Kết quả verify HMAC callback - Luồng 8 bước 8.3';
COMMENT ON COLUMN subscription_payments.is_anomaly IS 'True nếu số tiền callback không khớp - Luồng 8 bước 8.4a';
COMMENT ON COLUMN subscription_payments.confirmed_by IS 'Manager xác nhận thủ công khi is_anomaly = true';

-- ---------- 1. USERS & AUTH ----------

CREATE TABLE users (
  id                      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id               integer,
  email                   varchar NOT NULL,
  password_hash           varchar NOT NULL,
  full_name               varchar,
  role                    user_role NOT NULL,
  status                  user_status DEFAULT 'active',
  failed_login_attempts   integer DEFAULT 0,
  locked_until            timestamptz,
  created_at              timestamptz DEFAULT now(),
  updated_at              timestamptz,
  CONSTRAINT uq_users_email UNIQUE (email)
);
COMMENT ON TABLE users IS 'JWT sinh ra từ bảng này chứa user_id, role, tenant_id.';
COMMENT ON COLUMN users.tenant_id IS 'NULL nếu role = super_admin';

CREATE TABLE password_reset_tokens (
  id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     integer,
  token       varchar NOT NULL,
  expires_at  timestamptz NOT NULL,
  used        boolean DEFAULT false,
  created_at  timestamptz DEFAULT now(),
  CONSTRAINT uq_password_reset_tokens_token UNIQUE (token)
);
COMMENT ON TABLE password_reset_tokens IS 'Quên mật khẩu / đặt lại mật khẩu. Không có tenant_id trực tiếp - suy ra qua user_id.';

CREATE TABLE auth_sessions (
  id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     integer,
  jwt_id      varchar NOT NULL,
  issued_at   timestamptz,
  expires_at  timestamptz,
  revoked     boolean DEFAULT false,
  CONSTRAINT uq_auth_sessions_jwt_id UNIQUE (jwt_id)
);
COMMENT ON TABLE auth_sessions IS 'Theo dõi/thu hồi JWT khi đăng xuất. Không có tenant_id trực tiếp - suy ra qua user_id.';

-- ---------- 2. MARKETPLACE CONNECTION ----------

CREATE TABLE marketplace_connections (
  id                    integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id             integer,
  platform              platform_type NOT NULL,
  shop_id               varchar,
  shop_name             varchar,
  app_key               varchar,
  app_secret_encrypted  varchar,
  webhook_url           varchar,
  status                connection_status DEFAULT 'connected',
  auto_import_products  boolean DEFAULT false,
  connected_at          timestamptz DEFAULT now(),
  CONSTRAINT uq_marketplace_connections UNIQUE (tenant_id, platform, shop_id)
);
COMMENT ON TABLE marketplace_connections IS 'Một tenant có thể có nhiều shop trên cùng 1 sàn (VD: Mall + Outlet).';
COMMENT ON COLUMN marketplace_connections.app_secret_encrypted IS 'Mã hoá AES-256';

-- ---------- 3. CATALOG / PRODUCT ----------

CREATE TABLE products (
  id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id    integer,
  name         varchar NOT NULL,
  description  text,
  status       product_status DEFAULT 'active',
  created_by   integer,
  created_at   timestamptz DEFAULT now(),
  updated_at   timestamptz
);

CREATE TABLE product_skus (
  id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  product_id    integer,
  tenant_id     integer,
  sku_code      varchar NOT NULL,
  variant_name  varchar,
  base_price    numeric(14,2),
  created_at    timestamptz DEFAULT now(),
  CONSTRAINT uq_product_skus_code UNIQUE (tenant_id, sku_code)
);

CREATE TABLE product_images (
  id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  product_id  integer,
  sku_id      integer,
  tenant_id   integer,
  url         varchar NOT NULL,
  sort_order  integer DEFAULT 0,
  created_at  timestamptz DEFAULT now()
);
COMMENT ON TABLE product_images IS 'Lưu trên Cloud Storage theo prefix {tenant_id}/products/...';
COMMENT ON COLUMN product_images.sku_id IS 'NULL = ảnh chung sản phẩm; có giá trị = ảnh riêng của variant';

CREATE TABLE product_marketplace_links (
  id                     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sku_id                 integer,
  connection_id          integer,
  tenant_id              integer,
  platform               platform_type NOT NULL,
  platform_item_id       varchar,
  platform_product_url   varchar,
  platform_price         numeric(14,2),
  last_synced_at         timestamptz,
  created_at             timestamptz DEFAULT now(),
  CONSTRAINT uq_product_marketplace_links UNIQUE (sku_id, connection_id)
);
COMMENT ON TABLE product_marketplace_links IS 'Gắn link sản phẩm sàn TMĐT. Dùng connection_id thay vì platform để hỗ trợ nhiều shop/sàn.';

CREATE TABLE price_sync_configs (
  id                       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id                integer,
  scope                    sync_scope NOT NULL,
  sku_id                   integer,
  platform                 platform_type,
  sync_enabled             boolean DEFAULT false,
  sync_frequency_minutes   integer,
  created_at               timestamptz DEFAULT now()
);
COMMENT ON COLUMN price_sync_configs.sku_id IS 'NULL nếu scope = tenant';

CREATE TABLE price_sync_history (
  id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sync_config_id   integer,
  sku_id           integer,
  tenant_id        integer,
  platform         platform_type,
  old_price        numeric(14,2),
  new_price        numeric(14,2),
  is_anomaly       boolean DEFAULT false,
  confirmed_by     integer,
  changed_at       timestamptz DEFAULT now()
);
COMMENT ON TABLE price_sync_history IS 'Lịch sử & cảnh báo bất thường khi tự động fetch giá.';

-- ---------- 4. INVENTORY & OUTBOX ----------

CREATE TABLE inventory (
  id                 integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sku_id             integer,
  tenant_id          integer,
  quantity_on_hand   integer DEFAULT 0,
  reserved_quantity  integer DEFAULT 0,
  version            integer DEFAULT 0,
  updated_at         timestamptz,
  CONSTRAINT uq_inventory_sku UNIQUE (sku_id)
);
COMMENT ON TABLE inventory IS 'Giả định 1 kho duy nhất mỗi tenant (quan hệ 1-1 với product_skus).';
COMMENT ON COLUMN inventory.version IS 'Optimistic concurrency';

CREATE TABLE inventory_outbox_events (
  id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sku_id        integer,
  tenant_id     integer,
  platform      platform_type,
  quantity      integer,
  status        outbox_status DEFAULT 'pending',
  retry_count   integer DEFAULT 0,
  created_at    timestamptz DEFAULT now(),
  processed_at  timestamptz
);
COMMENT ON TABLE inventory_outbox_events IS 'Đồng bộ tồn kho ngược lên sàn TMĐT qua Outbox Pattern (Luồng 4).';

-- ---------- 5. ORDERS ----------

CREATE TABLE orders (
  id                    integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id             integer,
  connection_id         integer,
  platform              platform_type NOT NULL,
  platform_order_id     varchar,
  status                order_status DEFAULT 'pending',
  sla_deadline          timestamptz,
  shipping_method       varchar,
  tracking_number       varchar,
  courier_scanned_at    timestamptz,
  customer_name         varchar,
  customer_phone        varchar,
  shipping_address      text,
  total_amount          numeric(14,2),
  source                order_source,
  created_at            timestamptz DEFAULT now(),
  updated_at            timestamptz,
  CONSTRAINT uq_orders_platform_order UNIQUE (tenant_id, connection_id, platform_order_id)
);
COMMENT ON TABLE orders IS 'source=manual dùng cho đơn offline/in-store (Luồng 15), độc lập hoàn toàn với vòng đời marketplace.';
COMMENT ON COLUMN orders.sla_deadline IS 'Hạn chót Arrange Shipment - set khi status chuyển sang to_ship (Luồng 3 bước 3.9)';
COMMENT ON COLUMN orders.shipping_method IS 'pickup | drop_off - Luồng 3 bước 3.11';
COMMENT ON COLUMN orders.tracking_number IS 'Mã vận đơn (waybill)';
COMMENT ON COLUMN orders.courier_scanned_at IS 'Mốc thật để status tự động thành shipped';

CREATE TABLE order_items (
  id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  order_id    integer,
  tenant_id   integer NOT NULL,
  sku_id      integer,
  quantity    integer,
  unit_price  numeric(14,2)
);

CREATE TABLE order_status_history (
  id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  order_id     integer,
  tenant_id    integer NOT NULL,
  status       order_status,
  source       order_change_source NOT NULL,
  changed_by   integer,
  changed_at   timestamptz DEFAULT now()
);
COMMENT ON COLUMN order_status_history.source IS 'manager | marketplace_webhook | system - chặn Manager sửa đè trạng thái do sàn tự động đẩy';
COMMENT ON COLUMN order_status_history.changed_by IS 'NULL nếu source = marketplace_webhook/system';

CREATE TABLE returns_refunds (
  id                     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  order_id               integer,
  tenant_id              integer,
  trigger_type           return_trigger_type NOT NULL,
  return_tracking_number varchar,
  reason                 text,
  status                 refund_status DEFAULT 'requested',
  reviewed_by            integer,
  received_at            timestamptz,
  video_evidence_url     varchar,
  inspection_result      inspection_result_type DEFAULT 'pending',
  restocked              boolean DEFAULT false,
  dispute_status         dispute_status_type DEFAULT 'none',
  compensation_amount    numeric(14,2),
  created_at             timestamptz DEFAULT now()
);
COMMENT ON TABLE returns_refunds IS 'SmartOmni KHÔNG xử lý hoàn tiền cho khách - chỉ ghi nhận trạng thái + bằng chứng, tiền hoàn do sàn tự xử lý độc lập.';
COMMENT ON COLUMN returns_refunds.return_tracking_number IS 'Đối chiếu khi hàng về kho không khớp order_id';
COMMENT ON COLUMN returns_refunds.reviewed_by IS 'Manager duyệt/từ chối yêu cầu return - Luồng 9 bước 9.2a';
COMMENT ON COLUMN returns_refunds.received_at IS 'Thời điểm hàng vật lý về đến kho';
COMMENT ON COLUMN returns_refunds.video_evidence_url IS 'Bắt buộc - video đồng kiểm khi mở gói - Luồng 9 bước 9.5a';
COMMENT ON COLUMN returns_refunds.restocked IS 'Chỉ set true SAU KHI inspection_result = matched';
COMMENT ON COLUMN returns_refunds.compensation_amount IS 'Số tiền sàn đền bù nếu dispute_status = approved';

CREATE TABLE webhook_events_log (
  id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id        integer,
  platform         platform_type,
  event_id         varchar,
  event_type       varchar,
  order_id         integer,
  signature_valid  boolean,
  raw_payload      text,
  queued           boolean DEFAULT false,
  processed_at     timestamptz,
  received_at      timestamptz DEFAULT now(),
  CONSTRAINT uq_webhook_events_log UNIQUE (tenant_id, platform, event_id)
);
COMMENT ON TABLE webhook_events_log IS 'Log toàn bộ Webhook đến (cả tạo đơn lẫn đổi trạng thái), xác thực HMAC/SHA256.';
COMMENT ON COLUMN webhook_events_log.event_id IS 'Mã sự kiện do sàn cấp - idempotency cho Luồng 3b';
COMMENT ON COLUMN webhook_events_log.event_type IS 'order_created | shipped | delivering | completed | returned';
COMMENT ON COLUMN webhook_events_log.order_id IS 'NULL nếu chưa map được order (orphan event)';

CREATE TABLE seller_wallet_ledger (
  id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id     integer,
  order_id      integer,
  gross_amount  numeric(14,2),
  platform_fee  numeric(14,2),
  freeship_fee  numeric(14,2),
  net_payout    numeric(14,2),
  recorded_at   timestamptz DEFAULT now(),
  CONSTRAINT uq_seller_wallet_ledger_order UNIQUE (order_id)
);
COMMENT ON TABLE seller_wallet_ledger IS 'Ghi nhận payout tự động khi order.status = completed (Luồng 3b bước 3b.7a). Độc lập hoàn toàn với subscription_payments.';
COMMENT ON COLUMN seller_wallet_ledger.net_payout IS 'gross - platform_fee - freeship_fee';

-- ---------- 6. PROMOTIONS & AI ----------

CREATE TABLE promotion_events (
  id                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id         integer,
  name              varchar,
  discount_percent  numeric(5,2),
  start_date        date,
  end_date          date,
  applies_to        promotion_scope DEFAULT 'all',
  created_by        integer,
  created_at        timestamptz DEFAULT now()
);
COMMENT ON TABLE promotion_events IS 'Biến ngoại sinh h(t) khai báo cho mô hình Prophet.';

CREATE TABLE promotion_items (
  id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  promotion_id  integer,
  sku_id        integer,
  CONSTRAINT uq_promotion_items UNIQUE (promotion_id, sku_id)
);
COMMENT ON TABLE promotion_items IS 'Map 1 chương trình khuyến mãi với N sản phẩm. Không có tenant_id trực tiếp - suy ra qua promotion_id.';

CREATE TABLE ai_model_versions (
  id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id       integer,
  model_type      model_type NOT NULL,
  version_number  varchar,
  rmse            numeric(10,4),
  status          model_status DEFAULT 'training',
  trained_at      timestamptz DEFAULT now()
);
COMMENT ON TABLE ai_model_versions IS 'Registry tenant_id -> model version, phục vụ rollback và audit.';

CREATE TABLE demand_forecasts (
  id                       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id                integer,
  sku_id                   integer,
  model_version_id         integer,
  forecast_date            date,
  predicted_quantity       integer,
  recommended_reorder_qty  integer,
  created_at               timestamptz DEFAULT now(),
  CONSTRAINT uq_demand_forecasts UNIQUE (tenant_id, sku_id, forecast_date, model_version_id)
);
COMMENT ON TABLE demand_forecasts IS 'Kết quả Demand Forecasting - là điểm KẾT THÚC của Luồng 6 (Manager chỉ xem, không có module PO trong hệ thống).';

CREATE TABLE sales_outlier_exclusions (
  id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id    integer,
  sku_id       integer,
  order_id     integer,
  reason       varchar,
  excluded_at  timestamptz DEFAULT now(),
  CONSTRAINT uq_sales_outlier_exclusions UNIQUE (sku_id, order_id)
);
COMMENT ON TABLE sales_outlier_exclusions IS 'Ghi nhận dữ liệu lịch sử bị loại trước khi train Prophet (Luồng 6 bước 6.3) - phục vụ audit.';
COMMENT ON COLUMN sales_outlier_exclusions.order_id IS 'Đơn hàng bị coi là outlier (VD: 1 đơn sỉ số lượng bất thường)';
COMMENT ON COLUMN sales_outlier_exclusions.reason IS 'VD: quantity_deviation, one_time_bulk_order';

CREATE TABLE user_item_interactions (
  id                    integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id             integer,
  customer_identifier   varchar,
  sku_id                integer,
  interaction_type      interaction_type NOT NULL,
  weight                integer,
  created_at            timestamptz DEFAULT now()
);
COMMENT ON TABLE user_item_interactions IS 'User-Item Matrix cho Recommendation Engine (Luồng 12).';
COMMENT ON COLUMN user_item_interactions.customer_identifier IS 'session_id / anonymous id';
COMMENT ON COLUMN user_item_interactions.weight IS 'view=1, cart=3, purchase=5';

CREATE TABLE product_recommendations_cache (
  id                  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id           integer,
  source_sku_id       integer,
  recommended_sku_id  integer,
  similarity_score    numeric(6,4),
  computed_at         timestamptz DEFAULT now(),
  CONSTRAINT uq_product_recommendations_cache UNIQUE (tenant_id, source_sku_id, recommended_sku_id)
);
COMMENT ON TABLE product_recommendations_cache IS 'Top-3 gợi ý theo Cosine Similarity.';

CREATE TABLE redirect_click_events (
  id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id   integer,
  sku_id      integer,
  platform    platform_type,
  clicked_at  timestamptz DEFAULT now()
);
COMMENT ON TABLE redirect_click_events IS 'Ghi nhận click "Mua ngay" trên storefront (catalog-only, redirect ra sàn) để tính CTR - Luồng 2.';

-- ---------- 7. SUPPORT & SECURITY ----------

CREATE TABLE support_tickets (
  id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id    integer,
  created_by   integer,
  assigned_to  integer,
  subject      varchar,
  description  text,
  status       ticket_status DEFAULT 'open',
  created_at   timestamptz DEFAULT now(),
  resolved_at  timestamptz
);

CREATE TABLE security_audit_logs (
  id                 integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id          integer,
  event_type         security_event_type NOT NULL,
  details            text,
  session_tenant_id  integer,
  detected_at        timestamptz DEFAULT now()
);
COMMENT ON TABLE security_audit_logs IS 'Log RLS violation & kết quả kiểm thử chéo Tenant.';
COMMENT ON COLUMN security_audit_logs.tenant_id IS 'NULL nếu sự kiện không gắn tenant cụ thể';

CREATE TABLE system_metric_alerts (
  id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  metric_name      varchar,
  threshold_value  numeric(14,4),
  current_value    numeric(14,4),
  triggered_at     timestamptz DEFAULT now(),
  resolved         boolean DEFAULT false
);
COMMENT ON TABLE system_metric_alerts IS 'Cảnh báo hiệu năng/log hệ thống qua Prometheus/Grafana/ELK. Không gắn tenant - chỉ super_admin xem.';

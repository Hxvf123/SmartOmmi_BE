# SmartOmni - Kien truc chi tiet

## 1. So do luong Webhook (Real-time Sync)

```
Shopee/TikTok Shop
       │  POST /webhook/{tenantId}/{platform}
       ▼
API Gateway (resolve tenant tu path - FR-057)
       ▼
Integration Service
   ├─ Xac thuc chu ky HMAC/SHA256 (FR-058)
   ├─ Tra ve HTTP 200 trong 1-3s (FR-059)
   └─ Day message vao RabbitMQ (FR-060)
       ▼
   RabbitMQ (Durable Queue - FR-062)
       ▼
Order Service (Consumer)
   ├─ Kiem tra idempotency (unique tenant_id+platform+platform_order_id) (FR-025)
   ├─ Tao Order + OrderItem
   └─ Goi Inventory Service tru kho (FR-027)
       ▼
Inventory Service
   ├─ Optimistic Lock (@Version) - FR-027
   └─ Ghi Outbox Event (CUNG transaction) - FR-074
       ▼
   Outbox Publisher (scheduled) - FR-075
       ▼
   Goi lai API UpdateStock len Shopee/TikTok Shop - FR-076
```

## 2. Luong Polling du phong (Fallback Sync)

`OrderPollingScheduler` (trong Order Service) chay dinh ky 2-5 phut, quet
qua tung Tenant dang active, goi `IntegrationServiceClient.getOrderList(...)`,
doi chieu voi Database noi bo, va day cac don hang bi thieu vao lai
`OrderService.processIncomingOrder(..., OrderSource.POLLING)` — dung chung
co che idempotency voi luong Webhook.

## 3. Luong tu dong nhap san pham & dong bo gia (UC-40 -> UC-43)

```
Admin bat "auto_import_products" khi ket noi san (UC-18/UC-40)
       ▼
MarketplaceConnectionService.connect()
       ▼ (neu autoImportProducts = true)
ProductSyncService.importAllProducts()
   ├─ Goi MarketplaceClient.getProductList() (phan trang - FR-084)
   ├─ Upsert theo SKU (FR-085) qua Catalog Service
   └─ Dung khi vuot gioi han SKU (FR-086)

PriceSyncScheduler (chay dinh ky theo PriceSyncConfig - UC-42/UC-43)
   ├─ Goi MarketplaceClient.getCurrentPrice()
   ├─ Phat hien chenh lech bat thuong (FR-092)
   └─ Cap nhat gia rieng theo tung kenh (FR-091) + ghi log lich su (FR-093)
```

## 4. AI Service - ranh gioi Java/Python

`smartomni-service-ai` (Java) KHONG tu huan luyen mo hinh. No goi sang mot
**AI Microservice viet bang Python** (repo rieng, khong nam trong monorepo
Spring Boot nay) qua REST (`PythonAiServiceClient`), roi luu ket qua
(forecast, recommendation, model version + RMSE) vao PostgreSQL va expose
lai cho cac service Java khac / Admin ERP.

```
smartomni-service-ai (Java)  <---REST--->  AI Microservice (Python: FastAPI + Prophet/ARIMA + scikit-learn)
       │
       └─ Luu: ai_model_versions, demand_forecasts, product_recommendations_cache
```

## 5. Multi-tenancy & bao mat (UC-38, UC-39)

- **Tang ung dung**: moi entity nghiep vu ke thua `BaseTenantEntity` (co san
  `tenant_id`); `TenantContext` (ThreadLocal) mang tenant_id xuyen suot request.
- **Tang Database (khuyen nghi khi len production)**: bat PostgreSQL
  Row-Level Security (RLS) tren moi bang co `tenant_id`:

```sql
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON products
    USING (tenant_id = current_setting('app.current_tenant_id')::bigint);
```

  Moi Repository/Service can `SET app.current_tenant_id = ?` dau moi
  transaction (co the lam qua Hibernate Interceptor hoac
  `@PrePersist`/aspect doc tu `TenantContext`).

- **Feature Flag Enforcement (UC-41)**: `FeatureFlagService` (Tenant Service)
  cache ket qua qua Redis (`@Cacheable`), va PHAI duoc `@CacheEvict` ngay khi
  Tenant doi goi dich vu (FR-079).

## 6. Danh sach cong (port) mac dinh

| Service | Port |
|---|---|
| Gateway | 8080 |
| Auth | 8081 |
| Tenant | 8082 |
| Catalog | 8083 |
| Inventory | 8084 |
| Order | 8085 |
| Integration | 8086 |
| AI (Java gateway) | 8087 |
| AI Microservice (Python, repo rieng) | 8000 |

## 7. Viec can lam tiep (TODO tong hop)

- [ ] Trien khai that Shopee/TikTok Open API trong `ShopeeClient`/`TiktokShopClient`
- [ ] Feign client thuc su giua Catalog <-> Integration <-> Inventory <-> Tenant (hien dang comment TODO)
- [ ] Bat PostgreSQL Row-Level Security that su (hien tai chi enforce o tang application)
- [ ] Thay `ddl-auto: update` bang Flyway/Liquibase migration truoc khi len production
- [ ] Tich hop Eureka/Consul hoac Kubernetes Service Discovery thay cho URL hard-code
- [ ] Trien khai AI Microservice (Python) rieng, dong bo API contract voi `PythonAiServiceClient`
- [ ] Bo sung Unit Test / Integration Test (Testcontainers cho Postgres/RabbitMQ)

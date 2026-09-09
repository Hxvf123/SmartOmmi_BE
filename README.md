# SmartOmni Platform

Nền tảng bán lẻ đa kênh & tối ưu hóa chuỗi cung ứng tích hợp AI — kiến trúc **microservices multi-tenant**, core tech **Spring Boot 3 / Java 17**.

## Chiến lược tổ chức code: MONO-REPO

Toàn bộ 9 module (8 service + thư viện dùng chung) nằm trong **1 repo Git duy nhất** để team dễ quản lý tập trung:

- 1 lần `git clone` → thấy toàn bộ hệ thống, không phải gom nhiều repo.
- 1 lệnh `mvn clean install` (hoặc `make build`) → build hết tất cả module theo đúng thứ tự phụ thuộc.
- 1 pipeline CI (`.github/workflows/ci.yml`) → kiểm tra toàn bộ mỗi lần push.
- 1 file `docker-compose.yml` → dựng toàn bộ hạ tầng + service bằng `make up`.
- Sửa `smartomni-common` (JWT, exception, TenantContext...) → thấy ngay ảnh hưởng tới mọi service trong cùng 1 PR.

> Lưu ý: Mono-repo chỉ là cách **tổ chức code**, không ảnh hưởng kiến trúc **runtime** — mỗi module vẫn build ra 1 JAR/Docker image riêng, chạy port riêng, deploy/scale độc lập như microservices thông thường.

## 1. Kiến trúc tổng quan

```
                              ┌─────────────────────┐
                              │   API Gateway        │  (smartomni-gateway :8080)
                              │  - Resolve tenant_id  │
                              │  - Route theo service │
                              └──────────┬───────────┘
              ┌───────────┬──────────────┼──────────────┬──────────────┬────────────┐
              ▼           ▼              ▼              ▼              ▼            ▼
        ┌──────────┐┌──────────┐  ┌─────────────┐┌─────────────┐┌─────────────┐┌──────────┐
        │  Auth    ││ Tenant   │  │  Catalog     ││ Inventory   ││   Order     ││   AI     │
        │ :8081    ││ :8082    │  │  :8083       ││  :8084      ││   :8085     ││  :8087   │
        └──────────┘└──────────┘  └─────────────┘└──────┬──────┘└──────┬──────┘└──────────┘
                                                          │              │
                                                          ▼              ▼
                                                     ┌─────────────────────────┐
                                                     │  Integration Service     │
                                                     │  :8086 (Shopee/TikTok)   │
                                                     └─────────────────────────┘
                                    Hạ tầng dùng chung: PostgreSQL · Redis · RabbitMQ
```

| Module | Vai trò | UC/FR chính |
|---|---|---|
| `smartomni-common` | Thư viện dùng chung: TenantContext, JWT, exception handler, base entity | Cross-cutting |
| `smartomni-gateway` | API Gateway (Spring Cloud Gateway) — resolve tenant từ subdomain/path | UC-21 (webhook path) |
| `smartomni-service-auth` | Đăng nhập/đăng xuất/quên mật khẩu, JWT issuance | UC-01, 02, 03 |
| `smartomni-service-tenant` | Onboarding, gói dịch vụ, feature flag, resource usage, support ticket, security audit | UC-17→31, 34, 35 |
| `smartomni-service-catalog` | Sản phẩm, SKU, ảnh, link sàn TMĐT, khuyến mãi, cấu hình đồng bộ giá | UC-08, 09, 15, 16, 22, 23 |
| `smartomni-service-inventory` | Tồn kho, Outbox Pattern đồng bộ ngược sàn | UC-11, 40 |
| `smartomni-service-order` | Đơn hàng, hoàn/hủy, webhook nhận đơn, polling dự phòng | UC-10, 13, 32, 33, 34 |
| `smartomni-service-integration` | Kết nối Shopee/TikTok Shop, fetch sản phẩm/giá tự động | UC-18, 42, 43 |
| `smartomni-service-ai` | Lưu kết quả forecast/recommendation, gọi AI Service (Python) ngoài | UC-07, 14, 31, 35, 36, 37 |

> **Lưu ý:** AI core (Prophet/ARIMA, Collaborative Filtering) được huấn luyện bởi một **AI microservice viết bằng Python** (nằm ngoài repo Spring Boot này). `smartomni-service-ai` chỉ đóng vai trò Java gateway: gọi REST sang service Python, lưu kết quả, và expose API cho các service khác/Admin ERP.

## 2. Yêu cầu môi trường

- JDK 17+
- Maven 3.9+
- Docker & Docker Compose

## 3. Chạy dự án (local dev)

### Cách 1 — Chạy toàn bộ hệ thống bằng Docker (khuyến nghị khi mới clone)
```bash
make up        # build image + chạy tất cả (hạ tầng + 8 service)
make logs      # xem log
make down      # tắt toàn bộ
```

### Cách 2 — Chạy từng service qua IDE (khuyến nghị khi đang code/debug 1 service)
```bash
make infra-up  # chỉ bật Postgres, Redis, RabbitMQ

# rồi chạy 1 service cụ thể, ví dụ auth-service:
cd smartomni-service-auth && mvn spring-boot:run
# hoặc mở project ở thư mục gốc bằng IDE (IntelliJ/VSCode), mỗi module có Application class riêng
```

Xem `make help` để biết toàn bộ lệnh tiện ích (build, test, rebuild, ps...).

Mỗi service dùng chung 1 Postgres instance (database `smartomni`) trong giai đoạn dev; khi lên production, tách theo Database-per-Service theo đúng nguyên tắc microservices + Row-Level Security theo `tenant_id`.

## 4. Cấu trúc package chuẩn trong mỗi service

```
com.smartomni.<service>/
 ├── controller/     # REST API endpoints
 ├── service/        # Business logic
 ├── repository/     # Spring Data JPA repositories
 ├── entity/         # JPA entities (map theo DBML schema)
 ├── dto/            # Request/Response DTO
 ├── config/         # Security, Swagger, Beans config
 ├── scheduler/      # @Scheduled jobs (nếu có: polling, price sync...)
 ├── mq/             # RabbitMQ producer/consumer (nếu có)
 └── client/         # Feign/RestTemplate client gọi service khác (nếu có)
```

## 5. Quy tắc multi-tenant (bắt buộc đọc trước khi code)

1. Mọi entity nghiệp vụ **phải** kế thừa `BaseTenantEntity` (có sẵn `tenant_id`).
2. Mọi request vào hệ thống đi qua Gateway sẽ được gắn `tenant_id` vào header nội bộ `X-Tenant-Id` (từ subdomain hoặc JWT claim).
3. `TenantContext` (ThreadLocal, trong `smartomni-common`) dùng để lấy `tenant_id` hiện tại trong toàn bộ vòng đời request — **không** truyền tenant_id qua tham số thủ công.
4. Không viết native query thiếu điều kiện `tenant_id`; ưu tiên bật Hibernate Filter/PostgreSQL RLS ở tầng DB (xem `docs/architecture.md`).

## 6. Tài liệu liên quan

- `docs/architecture.md` — chi tiết kiến trúc, luồng Webhook/Polling, Outbox Pattern
- `docs/erd.dbml` — ERD đầy đủ (paste vào https://dbdiagram.io để xem trực quan)
- UC/FR specification — xem file Word đính kèm trong Google Drive/Confluence của team (không đưa vào repo code)

## 7. Quy trình đóng góp

Xem `CONTRIBUTING.md`.

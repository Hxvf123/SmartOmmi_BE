# Database migrations

## Cơ chế khởi động

Bảy service Auth, Tenant, Catalog, Inventory, Order, Integration và AI cùng phụ thuộc `smartomni-db-migrations`. Mỗi service chạy cùng bộ SQL trong `classpath:db/migration`, dùng chung `public.flyway_schema_history`. Nginx không kết nối database; Quartz worker hiện dùng memory job store cho local.

Flyway chạy trước khi Hibernate khởi tạo; Hibernate chỉ `validate`, không sửa schema. Khi nhiều instance khởi động đồng thời, Flyway khóa history để điều phối migration. Restart không chạy lại version đã áp dụng. Migration thất bại hoặc entity không khớp schema làm service khởi động thất bại.

| Version | Nội dung |
|---|---|
| V1 | 24 PostgreSQL enum, giá trị chữ thường |
| V2 | 36 bảng, ID/FK `bigint`, cột audit và tenant nullability |
| V3 | FK, unique constraints được khai báo trong V2, index bổ sung |
| V4 | Hàm RLS và quyền CRUD cho các bảng nghiệp vụ |
| V5 | ENABLE/FORCE RLS và policy tenant |
| V6 | Quyền giới hạn cho authentication, reset password và background jobs |
| V7 | Tài khoản khách, claim đơn hoàn tất, sổ điểm và kho mã voucher theo shop/sàn |
| V8 | 1.000 VND/điểm, hạng tích lũy, chương trình quà theo sàn và giữ chỗ mã không trừ điểm |

Tất cả service phải đóng gói cùng phiên bản migration. Khóa Flyway không giải quyết việc triển khai các JAR chứa bộ SQL khác nhau. Thay đổi schema cần tương thích với service đang chạy; ưu tiên thêm cột/bảng trước, chuyển code, rồi mới loại bỏ cấu trúc cũ trong một đợt riêng.

## Cấu hình và database mới

Copy `.env.example` thành `.env`, thay các placeholder bằng secret riêng, rồi chạy:

```sh
docker compose up -d postgres redis rabbitmq
docker compose up -d --build
docker compose logs -f auth-service
```

Compose truyền credentials vào từng service. Khi chạy qua IDE hoặc `mvn spring-boot:run`, phải đặt biến môi trường cho tiến trình Java; Spring Boot không tự đọc `.env` của Compose.

| Biến | Mục đích |
|---|---|
| `DB_URL` | JDBC URL; mặc định `jdbc:postgresql://localhost:5432/smartomni` ngoài Docker |
| `DB_MIGRATION_PASSWORD` | Mật khẩu role cố định `smartomni_migrator` |
| `DB_APP_PASSWORD` | Mật khẩu role cố định `smartomni_app` |
| `POSTGRES_PASSWORD` | Admin PostgreSQL trong Compose |
| `JWT_SECRET` | Secret chung của bảy service, ít nhất 32 byte |
| `SMARTOMNI_ENCRYPTION_AES_KEY` | Base64 của 32 byte ngẫu nhiên, dùng bởi Integration |

`scripts/db/00-bootstrap-roles.sh` chỉ tạo role, thiết lập mật khẩu và quyền schema khi PostgreSQL volume mới. Nó không tạo bảng nghiệp vụ. Không mount bộ SQL cũ vào `/docker-entrypoint-initdb.d` cùng cơ chế này.

Role migration sở hữu object và có DDL; role ứng dụng không là owner, không superuser, không BYPASSRLS, không được đọc/sửa Flyway history. Database và role phải tồn tại trước khi service chạy. Với PostgreSQL ngoài Docker, administrator thực hiện nội dung bootstrap qua `psql`, truyền mật khẩu bằng biến/secret, không sửa chúng vào SQL migration.

## RLS trong ứng dụng

`RlsJpaTransactionManager` thiết lập tenant/role bằng `set_config(..., true)` trên connection của mỗi transaction. Repository có transaction cho các truy vấn đọc; `open-in-view` được tắt. Thiếu context sẽ bị RLS từ chối hoặc trả về dữ liệu rỗng.

- Request lấy context từ JWT đã xác thực; `X-User-Role`, `X-User-Id`, `X-Tenant-Id` không cấp quyền.
- AuthService chỉ mở quyền cho email đang đăng nhập/quên mật khẩu hoặc user của reset token hợp lệ; không gán `super_admin` trước đăng nhập.
- Webhook chỉ tra connection của tenant trong path, rồi kiểm tra chữ ký trước enqueue. Phần chuẩn hóa payload webhook vẫn là TODO có sẵn.
- MQ consumer mở tenant scope trước khi vào transaction và khôi phục context trong `finally`.
- Outbox worker chỉ được đọc/cập nhật outbox; price-sync worker chỉ đọc marketplace connections. Không có quyền super-admin toàn schema.

Policy hiện tại yêu cầu super-admin để tạo tenant. Luồng tự đăng ký tenant ẩn danh và các API storefront cần thiết kế authorization riêng; không bypass RLS để hoàn thiện những TODO nghiệp vụ này. Các lời gọi service qua HTTP phải chuyển tiếp JWT phù hợp; header role/tenant đơn lẻ không còn đủ.

Migration chỉ tạo cấu trúc, không seed account hay subscription plan. Administrator phải provision dữ liệu khởi tạo phù hợp. Enum API của Order được đồng bộ với SQL: `PENDING`, `TO_SHIP`, `SHIPPED`, `DELIVERING`, `COMPLETED`, `CANCELLED`, `RETURNED`; client đang dùng `CONFIRMED`/`SHIPPING`/`DELIVERED` cần cập nhật. SKU của promotion được biểu diễn bằng bảng `promotion_items`, thay vì cột `sku_id` trên `promotion_events`.

## Database hiện có

Không xóa volume để chuyển đổi. Bootstrap chỉ chạy khi volume rỗng; với volume cũ phải provision role bằng tài khoản administrator hiện có. Nếu volume cũ được tạo bằng `POSTGRES_USER=smartomni`, dùng role đó để bootstrap, thay vì giả định role `postgres` tồn tại.

Quy trình cho **đúng bộ schema SQL 36 bảng trong `Database/files`**:

1. Backup bằng `pg_dump`, kiểm tra bản backup và dừng tất cả service ghi dữ liệu.
2. Provision `smartomni_migrator` và `smartomni_app` bằng admin hiện có.
3. Review rồi chạy `scripts/db/upgrade-legacy-schema.sql` bằng admin. Script giữ dữ liệu, đổi ID/FK/identity sequences sang bigint, thêm audit, chuyển ownership và dựng lại policy. Toàn bộ script là một transaction; tenant-owned rows có `tenant_id NULL` sẽ làm script thất bại để xử lý dữ liệu trước.
4. Chạy `scripts/db/verify-schema.sql`, kiểm tra số lượng dữ liệu trước/sau và đối chiếu schema trên bản clone thử nghiệm.
5. Đặt `DB_URL`, `DB_MIGRATION_PASSWORD` cho Maven, rồi chủ động baseline:

   ```sh
   mvn -pl smartomni-db-migrations flyway:baseline
   ```

   Plugin cấu hình baseline tại V5. Baseline chỉ đánh dấu V1–V5 đã có; **không thực thi hoặc tự xác minh chúng**.
6. Khởi động service để áp dụng V6–V8, rồi kiểm tra Hibernate validation, CRUD và cách ly tenant.

Script nâng cấp không dành cho schema do `ddl-auto: update` tự tạo hoặc database đã có Flyway history. Những database đó cần so sánh và SQL nâng cấp riêng. `baseline-on-migrate` luôn tắt; không dùng `repair` để che khác biệt schema/checksum.

## Thêm migration và kiểm thử

Thêm `V8__description.sql`, `V9__description.sql`, ... vào module chung; không sửa file đã áp dụng. Migration thêm bảng phải khai báo grants, policy RLS và index cần thiết; không có default grant cho mọi bảng, để bảo vệ metadata.

## Loyalty V7–V8

`customer_accounts` là tài khoản khách của từng tenant, tách khỏi `users` (nhân sự). `orders.completed_at` và `orders.eligible_for_points_at` phải được luồng đồng bộ/trạng thái điền khi đơn hoàn tất; V7 không giả định cố định số ngày chờ hoàn hàng. Đơn hoàn tất cũ chưa có các mốc này sẽ không claim được cho đến khi đối soát/backfill. Claim chỉ xét trạng thái `completed`, mốc đủ điều kiện đã qua và đơn chưa có claim; không kiểm tra chủ đơn. Unique `(tenant_id, order_id)` trong `loyalty_claims` quyết định người claim thành công đầu tiên. API phải yêu cầu khách đăng nhập, giới hạn thử mã, giấu thông tin đơn và phân biệt shop bằng `connection_id`.

V8 tính `floor(orders.total_amount / 1000)` lúc claim (ví dụ 500.999 VND → 500 điểm), lưu số tiền snapshot và không tin `points_awarded` từ client. Tổng điểm = điểm earn trừ reverse; đổi quà không trừ điểm. Hạng: dưới 500 `standard`, từ 500 `silver`, từ 1.000 `platinum`, từ 1.500 `diamond`. Hoàn hàng muộn ghi `reverse` và hạng được tính lại; mã đã cấp không tự thu hồi. Các dòng `redeem` V7 cũ được giữ làm lịch sử nhưng không còn tham gia tổng điểm. Bảng kho mã và issuance V7 chỉ dùng đọc dữ liệu cũ; quyền ứng dụng không được insert nữa.

Staff tạo `loyalty_reward_campaigns` cho một kết nối shop Shopee/TikTok, đặt `% giảm`, `required_points`, `quantity`, thời hạn và `provisioning_mode` (`api` hoặc `import`). Khách đủ tổng điểm chủ động tạo `loyalty_reward_requests`; trigger giữ một suất bằng `reserved_count` trong transaction, chống phát vượt số lượng và chống nhận lặp cùng chương trình. Worker gọi API sàn hoặc lấy mã đã chuẩn bị, rồi chuyển request sang `issued` kèm code; lỗi chuyển `failed` để nhả suất. `request_key` hỗ trợ đối soát/retry; phải xử lý idempotency phía adapter khi API sàn đã tạo mã nhưng ghi DB thất bại. Không gọi API sàn trong transaction database.

V7–V8 chỉ thay schema. Chưa có controller/customer authentication, đồng bộ thời điểm hoàn tất, chính sách cửa sổ hoàn hàng, worker/API voucher hay đối soát sử dụng code trên sàn. Backend phải xác thực quyền sở hữu `customer_id` của tài khoản web trong từng request; RLS hiện cô lập theo tenant nhưng không phân tách hai khách cùng tenant. Trước khi bật `provisioning_mode=api`, xác nhận endpoint tạo mã và scope thực tế cho từng sàn/shop. TikTok Shop công bố API đọc danh sách coupon nhưng tài liệu này nói coupon được tạo tại Seller Center/App; vì vậy cấu hình `import` là phương án khả dụng khi không có quyền tạo mã qua API.

```sh
mvn test
mvn -B clean verify
```

Module `smartomni-migration-tests` chạy PostgreSQL 16 tạm thời bằng native binaries, không cần Docker. `test` kiểm tra migration đồng thời, schema, mapping entity của bảy service, enum, auth/reset, RLS, rollback DDL và nâng cấp legacy. `verify` còn khởi động từng executable JAR của bảy service; MQ listener được tắt trong smoke test, nên đây không phải kiểm thử tích hợp RabbitMQ/Redis/marketplace, Nginx hoặc Quartz worker.

Log/report nằm trong `smartomni-migration-tests/target/`. PostgreSQL test dùng port tự chọn và dữ liệu riêng, không truy cập database phát triển. CI chạy `clean verify`; test phải chạy bằng user không phải root trên Linux. Lần đầu Maven tải PostgreSQL binaries và dependency kiểm thử.

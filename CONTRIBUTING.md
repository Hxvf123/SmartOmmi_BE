# Quy trình đóng góp - SmartOmni Platform

## Nhánh (Branching)
- `main` — luôn deployable, chỉ merge qua Pull Request đã review.
- `develop` — nhánh tích hợp chung.
- `feature/<service>-<mo-ta-ngan>` — vd `feature/order-webhook-idempotency`.
- `fix/<service>-<mo-ta-ngan>` — vd `fix/inventory-lock-bug`.

## Commit message
Theo Conventional Commits:
```
feat(order): thêm idempotency key khi nhận webhook Shopee
fix(inventory): sửa lỗi optimistic lock khi trừ kho đồng thời
docs(readme): cập nhật hướng dẫn chạy local
```

## Pull Request checklist
- [ ] Code build thành công: `mvn clean install`
- [ ] Đã viết/](cập nhật unit test cho logic mới
- [ ] Không hardcode `tenant_id`; đã dùng `TenantContext`
- [ ] Đã cập nhật `README.md`/`docs/` nếu thay đổi kiến trúc hoặc API
- [ ] Không commit file `.env`, secret, credentials

## Coding convention
- Java 17, Spring Boot 3.x
- Package theo layer: `controller / service / repository / entity / dto / config`
- DTO tách biệt hoàn toàn với Entity (không expose JPA entity qua API)
- Toàn bộ exception nghiệp vụ kế thừa `BusinessException` (trong `smartomni-common`) để `GlobalExceptionHandler` xử lý thống nhất
- Response API dùng wrapper chung `ApiResponse<T>`

## Review
Mỗi PR cần tối thiểu 1 approval từ thành viên khác trước khi merge vào `develop`.

## Database migration
- Thay đổi schema bằng file `V<n>__description.sql` trong `smartomni-db-migrations/src/main/resources/db/migration/`; không sửa migration đã áp dụng.
- Giữ Hibernate `ddl-auto: validate`, không bật `update` hoặc tự baseline database hiện có.
- Thêm grants, RLS policy và index khi thêm bảng; triển khai các service với cùng phiên bản migration.
- Chạy `mvn clean verify` để kiểm thử PostgreSQL 16 và khởi động JAR từng service. Xem `docs/database-migrations.md` để cấu hình và chuyển schema cũ.

# A2 — Thu hồi access token khi khóa tài khoản

## Mục tiêu

Sau khi chuyển user sang `DISABLED`, mọi access token đã cấp cho user bị từ chối trên mọi service ngay ở request kế tiếp. Mở khóa không hồi sinh token cũ. Đổi role cũng làm token mang role cũ mất hiệu lực.

## Thiết kế cần chốt khi triển khai

- Dùng phiên bản phiên (`tokenVersion`) gắn với user, phát hành trong JWT và kiểm tra tại filter chung. Nguồn sự thật nên nằm ở DB; Redis là nguồn tra cứu nhanh cho mọi service. Cập nhật DB/Redis phải có chiến lược nhất quán, kể cả khi Redis lỗi sau commit.
- Kiểm tra `status` và `tokenVersion` trước khi nạp `SecurityContext`. Không chấp nhận token cũ thiếu claim sau khi chuyển sang cơ chế mới; cần kế hoạch rollout/relogin cho người dùng đang đăng nhập.
- Không dựa vào blacklist từng token nếu không có danh mục tất cả token trên mọi thiết bị.

## Phase 1 — Lưu và cấp phiên bản token

1. Tạo migration mới thêm `token_version` không null, mặc định `0` cho bảng user; không sửa migration cũ. Xác nhận tên bảng/entity hiện hành và RLS trước khi viết migration.
2. Cập nhật entity, `JwtTokenProvider` và luồng login/register/refresh để JWT mang phiên bản hiện hành.
3. Test token mới có claim đúng, tăng phiên bản không làm hỏng việc parse các claim cũ, migration chạy được trên PostgreSQL 16.
4. Chạy `mvn -pl smartomni-service-auth,smartomni-db-migrations,smartomni-migration-tests -am test` và `mvn -pl smartomni-migration-tests -am verify` nếu hạ tầng test sẵn sàng.
5. Test đạt → kiểm tra diff/stage → commit `feat(auth): issue versioned access tokens`.

## Phase 2 — Kiểm phiên bản và trạng thái ở mọi service

1. Đưa logic kiểm tra vào `JwtAuthenticationFilter` trong `smartomni-common`, được cả auth và domain services sử dụng. Tra cứu trạng thái/phiên bản theo user ID từ nguồn chung; chỉ chấp nhận `ACTIVE` và phiên bản trùng.
2. Thiết kế fail-closed khi không xác định được trạng thái user; tránh Redis lỗi khiến user bị khóa tiếp tục truy cập. Chú ý public endpoint và service-to-service authentication để không làm đứt luồng nội bộ.
3. Từ chối token thiếu claim phiên bản theo chính sách rollout; trả 401 có mã lỗi phân biệt token bị thu hồi. Kiểm tra cả cache, TTL và tình huống Redis không có key.
4. Test filter ở auth và ít nhất một domain service: active/trùng version qua; disabled/sai version/không tra được bị chặn; public endpoint vẫn hoạt động.
5. Chạy `mvn -pl smartomni-common,smartomni-service-auth,smartomni-service-order -am test`. Test đạt → kiểm tra diff/stage → commit `fix(auth): reject disabled and stale access tokens`.

## Phase 3 — Tăng phiên bản khi khóa/đổi role

1. Trong `UserServiceImpl.updateUserStatus`, khi trạng thái thực sự đổi sang `DISABLED`, tăng `tokenVersion` theo cách an toàn khi cập nhật đồng thời; đồng bộ nguồn tra cứu của filter. Mở khóa không giảm phiên bản.
2. Trong `assignRoles`, tăng phiên bản sau khi đổi role để JWT cũ không giữ quyền cũ. Rà các đường khác đổi status/role và áp cùng quy tắc.
3. Tính đến khoảng thời gian DB commit nhưng Redis chưa cập nhật: dùng cơ chế after-commit/outbox hoặc cập nhật có kiểm soát, và ghi rõ cách retry khi đồng bộ thất bại. Không trả thành công nếu hệ thống vẫn có thể chấp nhận token bị thu hồi.
4. Test login A, khóa A, token A bị từ chối trên nhiều service; user B không bị ảnh hưởng; mở khóa A, token cũ vẫn bị từ chối; login mới hoạt động; đổi role vô hiệu hóa token role cũ.
5. Chạy `mvn -pl smartomni-service-auth,smartomni-common -am test` và bài kiểm thử tích hợp liên service. Test đạt → kiểm tra diff/stage → commit `fix(auth): invalidate sessions on status and role changes`.

## Hoàn tất khi

Không có đường xác thực JWT nào chỉ dựa vào chữ ký và hạn dùng mà bỏ qua trạng thái/phiên bản user; không có cửa sổ 24 giờ cho user đã khóa.

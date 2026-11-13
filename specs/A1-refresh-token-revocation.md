# A1 — Thu hồi đúng refresh token của user

## Mục tiêu

Khóa hoặc đổi role của user ID `1` chỉ thu hồi refresh token của chính user đó; token của user `10`, `11`, `100` vẫn dùng được. Token của user bị khóa không thể refresh.

## Hiện trạng

`UserServiceImpl.updateUserStatus` và `assignRoles` dùng Redis pattern `auth:user_refresh:<id>*`. Mỗi user hiện có key xuôi `auth:user_refresh:<id> -> token` và key ngược `auth:refresh_token:<token> -> id`. Chỉ xóa key xuôi sẽ để key ngược còn hiệu lực.

## Phase 1 — Tạo thao tác thu hồi chính xác

1. Tạo hàm dùng chung nhận user ID; đọc đúng key `auth:user_refresh:<id>` (không wildcard), lấy token rồi xóa cả `auth:refresh_token:<token>` và `auth:user_refresh:<id>`.
2. Đảm bảo thao tác idempotent khi không có token. Xem xét Lua script/transaction Redis để tránh race với refresh đang rotate token; không dùng `KEYS` trên Redis production.
3. Test user `1`, `10`, `11`, `100`: chỉ hai key liên quan đến `1` bị xóa; các key khác giữ nguyên; không có token cũng xử lý được.
4. Chạy `mvn -pl smartomni-service-auth -am test`. Test đạt → kiểm tra diff/stage → commit `fix(auth): revoke exact user refresh token keys`.

## Phase 2 — Áp dụng cho khóa và đổi role

1. Thay logic wildcard trong `updateUserStatus` và `assignRoles` bằng hàm chung. Rà logout, đổi mật khẩu, khóa tenant để dùng cùng quy tắc nơi phù hợp.
2. Giữ kiểm tra `DISABLED` trong refresh API như lớp bảo vệ bổ sung. Phối hợp với A2 để access token cũ cũng bị vô hiệu hóa.
3. Test khóa user `1`/đổi role user `1`: refresh của `1` thất bại; refresh user `10` thành công; không xóa nhầm key và không để key ngược mồ côi.
4. Chạy `mvn -pl smartomni-service-auth -am test`. Test đạt → kiểm tra diff/stage → commit `fix(auth): use scoped refresh revocation for user changes`.

## Hoàn tất khi

Không còn wildcard theo ID user trong đường thu hồi refresh token; cả hai chiều ánh xạ được xóa đúng user.

# Kế hoạch xử lý A1–A5

Thư mục này chứa năm kế hoạch triển khai độc lập:

1. [A3 — Cách ly tồn kho theo tenant](A3-inventory-tenant-isolation.md)
2. [A4 — Cách ly đơn hàng và hoàn trả](A4-order-tenant-isolation.md)
3. [A2 — Thu hồi access token khi khóa tài khoản](A2-access-token-revocation.md)
4. [A1 — Thu hồi đúng refresh token](A1-refresh-token-revocation.md)
5. [A5 — Kiểm soát chuyển trạng thái đơn](A5-order-status-transitions.md)

## Quy tắc thực hiện chung

- Làm lần lượt theo thứ tự trên; A2 và A1 cùng sửa auth nên kiểm tra xung đột trước khi bắt đầu phase tiếp theo.
- Mỗi phase phải tạo ra một thay đổi code hoàn chỉnh và chạy test ghi trong phase. **Chỉ khi test đạt mới commit phase đó**, dùng Conventional Commit và chỉ stage đúng code/test/migration/config của phase. Nếu test lỗi, sửa và chạy lại trước khi commit. Không gộp nhiều phase vào một commit.
- Trước từng commit, xem `git status`, diff và kết quả test; stage đúng file của phase và commit ngay khi test đạt, không cần chờ duyệt.
- Bộ kế hoạch Markdown trong `specs/` được đưa vào Git theo yêu cầu trực tiếp của chủ repo. Khi triển khai, mỗi commit phase chứa code/test/migration/config tương ứng và cập nhật plan nếu cần.
- Không sửa migration đã áp dụng. Nếu cần thay schema, tạo migration mới và chạy `smartomni-migration-tests`.
- Giữ nguyên thay đổi có sẵn trong workspace của người khác; không dùng `git add .`.

# A5 — Chặn chuyển trạng thái đơn không hợp lệ

## Mục tiêu

Không lưu trạng thái sai, không ghi lịch sử và không kích hoạt hoàn kho/ghi doanh thu cho yêu cầu bị từ chối. Staff chỉ được chuyển `PENDING -> TO_SHIP`; webhook sàn dùng quy tắc riêng.

## Phase 1 — Chặn chuyển trạng thái do Staff yêu cầu

1. Trong `OrderService.updateStatus`, sau khi tìm đơn theo tenant (A4), kiểm tra nguồn `MANAGER`: chỉ cho `PENDING -> TO_SHIP`; request giữ nguyên trạng thái có thể trả kết quả idempotent.
2. Trả `BusinessException` HTTP 409 với mã ổn định như `INVALID_ORDER_STATUS_TRANSITION` trước mọi lệnh `save`, ghi history, restock hoặc wallet ledger.
3. Đảm bảo controller không cho client giả mạo `source`; `updateStatus` luôn gắn nguồn `MANAGER` từ server. Kiểm tra phân quyền Staff/Admin ở endpoint nếu chưa được áp dụng.
4. Test chuyển hợp lệ, chuyển sai, giữ nguyên trạng thái; chuyển sai không tạo history/outbox/restock/ledger.
5. Chạy `mvn -pl smartomni-service-order -am test`. Test đạt → kiểm tra diff/stage → commit `fix(order): restrict staff status transitions`.

## Phase 2 — Kiểm state machine cho webhook

1. Trong `updateOrderStatus`, thay nhánh `log.warn` rồi vẫn lưu bằng kiểm tra ném lỗi hoặc chính sách bỏ qua có audit cho sự kiện trễ/trùng. Quy tắc webhook độc lập với quy tắc Staff.
2. Rà `TiktokOrderStatusMapper.isValidTransition`, trạng thái cuối, sự kiện đến sai thứ tự, và nhánh mapper fallback trạng thái lạ về `PENDING`; không để trạng thái lạ làm đơn bị lùi.
3. Giữ idempotency cho sự kiện trùng; xác định retry/dead-letter cho sự kiện sàn bị từ chối. Không chặn `COMPLETED -> RETURNED` nếu quy trình hoàn trả từ sàn cho phép.
4. Test webhook tiến hợp lệ, trùng, đến trễ, trạng thái lạ, chuyển khỏi trạng thái cuối; xác nhận không có tác động phụ cho sự kiện bị từ chối.
5. Chạy `mvn -pl smartomni-service-order -am test` và kiểm thử consumer/webhook liên quan. Test đạt → kiểm tra diff/stage → commit `fix(order): enforce marketplace status transitions`.

## Hoàn tất khi

Không còn đường `log.warn` rồi lưu chuyển trạng thái sai; hành động Staff và sự kiện sàn có luật rõ ràng, test riêng, không ghi tác động phụ khi bị từ chối.

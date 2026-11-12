# A4 — Cách ly đơn hàng và hoàn trả theo tenant

## Mục tiêu

Người dùng tenant A không thể thay đổi trạng thái đơn, tạo/duyệt hoàn trả hoặc tác động dữ liệu của tenant B bằng cách đoán ID.

## Phase 1 — Chặn cập nhật trạng thái đơn chéo tenant

1. Thêm `findByIdAndTenantId` vào `OrderRepository`.
2. Trong `OrderService.updateStatus`, yêu cầu tenant từ `TenantContext`, truy vấn theo ID + tenant; nếu thiếu tenant hoặc không thấy đơn thì từ chối trước mọi ghi.
3. Giữ luồng webhook riêng: dùng tenant đã xác minh từ kết nối/sự kiện, không dựa vào tenant context của Staff.
4. Test tenant A gọi ID đơn của B nhận 404, không ghi history/ledger/restock; test cùng tenant vẫn thành công.
5. Chạy `mvn -pl smartomni-service-order -am test`. Test đạt → review/stage → commit `fix(order): scope manual status updates to tenant`.

## Phase 2 — Chặn tạo/duyệt hoàn trả chéo tenant

1. Trong `ReturnRefundService.createReturnRequest`, tìm order bằng ID + tenant trước khi tạo refund.
2. Thêm truy vấn refund theo ID + tenant; dùng trong `approveAndRestock`, rồi lấy order liên quan cũng theo ID + tenant. Kiểm tra cả item dùng để restock thuộc order/tenant đó.
3. Trả 404 cho ID ngoài phạm vi tenant; không tiết lộ đơn/refund của shop khác. Không tin tenantId trong request body.
4. Test tạo/duyệt refund chéo tenant không ghi refund, không đổi order, không restock; test cùng tenant hoạt động.
5. Chạy `mvn -pl smartomni-service-order -am test`. Test đạt → review/stage → commit `fix(order): scope returns and refunds to tenant`.

## Phase 3 — Rà các đường ghi đơn còn lại

1. Kiểm tra `updateOrderTracking`, `handleOrderReturn` và các controller khác gọi chúng; phân loại đường Staff và đường webhook/nội bộ.
2. Áp cùng quy tắc tenant cho các đường Staff; đường nội bộ lấy tenant từ nguồn đã xác thực. Giữ endpoint `/track` là bài toán quyền truy cập riêng, không biến tra cứu công khai thành cập nhật.
3. Thêm test hồi quy cho từng đường ghi được sửa; chạy `mvn -pl smartomni-service-order -am test` và migration/RLS tests nếu thay truy vấn hoặc schema liên quan.
4. Test đạt → review/stage → commit `fix(order): enforce tenant scope on remaining writes`.

## Hoàn tất khi

Mọi đường ghi đơn/refund do Staff gọi đều giới hạn theo tenant; đường webhook dùng tenant đã xác minh; không có tác động phụ khi ID thuộc tenant khác.

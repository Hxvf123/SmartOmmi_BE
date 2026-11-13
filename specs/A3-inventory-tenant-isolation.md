# A3 — Cách ly tồn kho theo tenant

## Mục tiêu

Mọi thao tác `deductStock`, `adjustStock`, `restock` chỉ tác động đến SKU và bản ghi tồn kho thuộc tenant đã xác thực. SKU sai tenant/không tồn tại không tạo inventory hoặc outbox event.

## Hiện trạng và quyết định

- `InventoryService` dùng `findBySkuId`; `adjustStock` và `restock` tự tạo dòng. `InventoryRepository` chưa có truy vấn theo tenant.
- Schema đã có unique `inventory(sku_id)` và FK `inventory.sku_id -> product_skus.id` trong V2/V3. **Không tự động thêm unique `(tenant_id, sku_id)`**; cần xác minh dữ liệu và ràng buộc hiện có trước khi thêm migration.
- Lời gọi Order → Inventory qua Feign hiện chỉ truyền SKU/số lượng. Phải xác định cơ chế truyền danh tính tenant đáng tin cậy; không tin `X-Tenant-Id` do client tự gửi. Luồng webhook/background cần danh tính service và tenant đã được xác minh.

## Phase 1 — Đóng truy cập chéo tenant khi đọc tồn kho

1. Thêm `findByTenantIdAndSkuId` trong `InventoryRepository`.
2. Bắt buộc tenant context hợp lệ ở ba thao tác; thay toàn bộ `findBySkuId` bằng truy vấn theo cặp tenant/SKU. Không sử dụng fallback toàn hệ thống.
3. Khi không thấy dòng tồn kho, cả ba thao tác tạm thời trả lỗi; không tạo dòng mới cho đến khi phase 2 có xác minh SKU. Như vậy phase 1 đã đóng lỗ hổng trước khi commit.
4. Thêm test tenant A không sửa được tồn kho tenant B và không ghi outbox; test thao tác hợp lệ cùng tenant.
5. Chạy `mvn -pl smartomni-service-inventory -am test`. Test đạt → xem diff/stage chính xác → commit `fix(inventory): scope inventory lookups to tenant`.

## Phase 2 — Xác minh SKU trước khi tạo tồn kho

1. Chọn một đường xác minh SKU từ Catalog Service theo **tenantId + skuId**; nếu cần endpoint nội bộ mới, chỉ cho service đã xác thực gọi. Tránh kiểm tra bằng `skuId` đơn lẻ.
2. Trong `adjustStock`/`restock`, chỉ tạo inventory khi Catalog xác nhận SKU cùng tenant. SKU không tồn tại/sai tenant trả 404; lỗi Catalog không được tự động coi là SKU hợp lệ.
3. Đảm bảo tạo inventory và outbox chỉ sau khi xác minh. Kiểm tra race khi hai request tạo cùng SKU; dùng unique hiện có và xử lý xung đột rõ ràng.
4. Test SKU sai tenant, SKU không tồn tại, SKU hợp lệ chưa có inventory, và lỗi Catalog. Chạy `mvn -pl smartomni-service-catalog,smartomni-service-inventory -am test` cùng migration test nếu thay schema.
5. Test đạt → review/stage → commit `fix(inventory): validate sku ownership before creation`.

## Phase 3 — Bảo toàn luồng Order → Inventory

1. Cập nhật Feign client/cấu hình để lời gọi trừ/hoàn kho mang tenant đã xác minh của đơn và xác thực service; xác nhận filter của Inventory nhận đúng context. Không mở endpoint công khai chỉ để truyền tenant.
2. Kiểm tra mọi nguồn gọi: đơn mới từ webhook, hủy đơn và hoàn trả. Khi Inventory từ chối, không âm thầm báo toàn bộ xử lý thành công; xác định retry/đối soát cho luồng bất đồng bộ.
3. Test tích hợp Order → Inventory với tenant đúng/sai/thiếu; xác nhận không đổi kho tenant khác và không tạo outbox sai tenant.
4. Chạy `mvn -pl smartomni-service-order,smartomni-service-inventory -am test`; nếu có integration test cần hạ tầng, chạy `mvn -B clean verify` theo khả năng môi trường và ghi rõ kết quả.
5. Test đạt → review/stage → commit `fix(inventory): propagate verified tenant for order stock calls`.

## Hoàn tất khi

Không còn đường ghi tồn kho theo `skuId` đơn lẻ; mọi đường tạo dòng xác minh SKU; luồng Order vẫn trừ/hoàn kho đúng tenant.

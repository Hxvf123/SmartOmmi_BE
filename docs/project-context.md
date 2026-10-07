# SmartOmni — ghi nhớ quyết định sản phẩm

*Cập nhật 05/10/2026 từ câu trả lời trực tiếp của chủ dự án. Dùng để đọc lại bối cảnh khi tiếp tục thiết kế. Đây là bản planning; khi tài liệu cũ, migration hoặc mã nguồn khác với quyết định dưới đây, ghi rõ khác biệt trước khi sửa thiết kế.*

## Mục tiêu và phạm vi

- SmartOmni là hệ thống quản trị bán lẻ đa kênh cho tenant. Mỗi tenant có một cửa hàng logic, tối đa một tài khoản shop Shopee và một tài khoản shop TikTok Shop, cùng một storefront riêng. Không cho hai tài khoản shop trên cùng một sàn trong cùng tenant.
- SmartOmni thu thập dữ liệu qua API của từng sàn; luồng webhook và polling dùng để cập nhật/bù dữ liệu. Storefront chỉ giới thiệu sản phẩm, AI chatbot tư vấn và chuyển khách sang sàn để mua; không checkout hay thu tiền đơn tại storefront.
- Phạm vi bản đầu mà chủ dự án nêu: đơn hàng, tồn kho, catalog, AI và thanh toán. Thanh toán SmartOmni chỉ dành cho **gói SaaS của tenant**. Đồng bộ tin nhắn và tích điểm được đặc tả như nghiệp vụ mục tiêu; thời điểm đưa vào bản đầu chưa được chốt lại sau câu trả lời này.
- Tài liệu cần mô tả nghiệp vụ mục tiêu đầy đủ ở mức planning. **Ưu tiên thiết kế tiếp theo**: phân tích chỉ số kinh doanh trên dashboard và AI tư vấn chiến lược kinh doanh. Chưa yêu cầu triển khai code.

## Người dùng và giao diện

| Nhóm người dùng | Giao diện | Nhiệm vụ chính |
|---|---|---|
| Customer | Storefront và Customer Portal riêng | Xem sản phẩm, chuyển sang sàn, AI chatbot Storefront, tạo tài khoản và tích điểm. |
| Staff | Merchant Portal | Quản lý sản phẩm/SKU và URL sàn, xử lý đơn, điều chỉnh tồn kho, in vận đơn PDF, nhận cảnh báo AI, trả lời hội thoại sàn. |
| Tenant Admin | Merchant Portal | Tự tạo tenant, cấu hình kết nối sàn và quyền Staff, xem BI, quản lý gói SaaS, chủ động phát voucher. |
| Super Admin | Super Admin Console | Quản trị tenant, APM, khóa/mở vi phạm, gói dịch vụ và giám sát RLS. |

Storefront là một giao diện/hệ thống, không phải actor người thứ năm. Trong tài liệu cũ có tên “Manager”; ở quyết định mới, nhóm vận hành là **Staff**. Chưa chốt Staff có nhiều cấp quyền con hay không.

## Tin nhắn và CSKH

- Chỉ đồng bộ hội thoại **khách ↔ shop trên Shopee/TikTok Shop**. Staff đọc và trả lời trong hộp thư chung tại Merchant Portal. Nếu shop/app không có quyền API đọc/gửi tin, không hỗ trợ kênh đó.
- Dùng webhook và polling bù, không nhập hội thoại lịch sử trước lúc kết nối. Hội thoại tách theo sàn/shop/khách, kể cả khi hai shop cùng tenant. Không đồng bộ danh tính khách giữa các sàn.
- Mong muốn hỗ trợ văn bản, ảnh, video, tệp, sticker, thẻ sản phẩm và thẻ đơn, nhưng khả năng thực tế tùy loại nội dung API sàn cấp.
- AI trong hộp thư **chỉ phân loại tin thường/khiếu nại**. Ảnh tham chiếu mô tả session router, phiên khiếu nại bị khóa và bỏ qua AI cho tin sau cho đến khi Staff giải quyết. Chủ dự án trả lời rõ AI không hỗ trợ soạn/tự trả lời ở tính năng này, nên nhánh “AI Copilot Drafts” trong ảnh không đưa vào luồng. AI chatbot của Storefront là sản phẩm riêng.
- Nhiều Staff có thể mở một hội thoại; Staff chịu trách nhiệm trả lời. Toàn bộ thao tác cần audit. Tin nhắn lưu không đặt thời hạn xóa tự động.
- Nếu gửi thất bại/timeout: UI hiển thị lỗi hoặc trạng thái chưa rõ và monitoring ghi sự kiện. Đề xuất ngưỡng, **chưa phải quyết định của chủ dự án**: 5 phút để cảnh báo đồng bộ chậm; 15 giây để báo lệnh gửi chưa có kết quả.
- Khi kết nối sàn lỗi/hết quyền: email Tenant Admin và hiển thị cảnh báo đăng nhập cho Tenant Admin lẫn Staff.
- Liên kết hội thoại với lịch sử mua hàng bằng ID khách của **cùng sàn và shop** nếu xác minh được trường ID tương ứng giữa API chat và order. TikTok Get Conversation trả `user_id` và `im_user_id`, Order List lọc `buyer_user_id`; giả thuyết nối `user_id` ↔ `buyer_user_id` cần kiểm thử, không tự suy ra `im_user_id` tương đương. Shopee chưa xác minh trường ID chính thức qua tài liệu truy cập được.
- Tải giả định: 100 đơn và **khoảng 500 tin/ngày/tenant**; “5p0” trong câu trả lời được tạm hiểu là 500, chưa phải số đo thực tế.

## Khách hàng, điểm và hạng

- Customer Portal riêng cho khách đăng ký bằng email và số điện thoại. Không gộp danh tính khách Shopee/TikTok. Một tài khoản Customer Portal tích điểm chung cho các shop trong **cùng tenant**.
- Khách chọn shop, nhập mã đơn và claim **thủ công**. Người claim hợp lệ đầu tiên được điểm; **không xác minh họ có phải người mua trên sàn**. Đây là quyết định nghiệp vụ đã được xác nhận, cần audit và giới hạn thử mã.
- Đơn do sàn xác nhận hoàn tất; sau **15 ngày kể từ `completed_at`** mới đủ điều kiện claim. Staff/Tenant Admin không sửa tay trạng thái `completed`/`returned`. Theo giả định nghiệp vụ của chủ dự án, qua 15 ngày không cần quy trình đảo điểm vì sàn đã hết thời hạn xử lý hoàn/trả.
- Điểm = `floor(tiền hàng khách thực trả / 1.000 VND)` sau giảm giá, **không tính vận chuyển và thuế**. Tỷ lệ cố định, tenant/shop không tùy chỉnh. Nếu API không cho tách giá trị chính xác, cần đối soát thay vì tính từ tổng tiền chưa rõ cấu phần.
- Điểm không hết hạn, không tiêu để lấy thưởng. Hạng dựa trên tổng điểm tích lũy: `standard` < 500, `silver` ≥ 500, `platinum` ≥ 1.000, `diamond` ≥ 1.500. Quy tắc thông thường không hạ hạng.
- Không có yêu cầu đổi thưởng từ Customer, không giữ suất thưởng theo yêu cầu của Customer. Tenant Admin tự chọn khách/hạng và quyết định có phát voucher hay không.
- Mục tiêu: SmartOmni gọi API sàn để tạo voucher cho đúng shop, lưu kết quả và gửi email thông báo mã cho khách. Khách dùng mã trên ứng dụng Shopee/TikTok. Thuộc tính voucher và khả năng audit việc sử dụng phụ thuộc API sàn. Nếu API không hỗ trợ tạo, chức năng phát qua SmartOmni không khả dụng cho sàn đó; chưa được tự đổi sang cơ chế nhập mã thủ công.
- Khi API tạo voucher timeout, cần đối soát trước retry để một yêu cầu không tạo hai voucher ngoài sàn. Chỉ email khách khi đã xác nhận mã được tạo.

## Điểm khác với schema/tài liệu hiện có

- V7/V8 hiện tính claim từ `orders.total_amount`; yêu cầu mới là **tiền hàng thực trả, không ship/thuế**. Cần xác định trường/snapshot nguồn và migration mới khi triển khai.
- V8 có `loyalty_reward_campaigns` và `loyalty_reward_requests` cho khách chủ động yêu cầu thưởng, nhưng quyết định mới là **Admin phát mã chủ động, khách không đổi thưởng**. Không dùng luồng reward request này trong activity mục tiêu. Không sửa migration đã áp dụng; lên kế hoạch schema/migration tiếp theo.
- V7/V8 có `reverse` và hạ hạng khi điểm giảm; quy tắc mục tiêu thông thường không cần đảo điểm sau 15 ngày. Ngoại lệ hoàn/trả sau 15 ngày chưa được quyết định.
- Các service hiện tại chưa có API Customer Portal, worker cấp voucher, email thông báo, lưu tin nhắn hoặc session CSKH. Đây là phần khung/tham chiếu, không được mô tả như luồng đang chạy.
- Tài liệu TikTok [Search Coupon List](https://partner.tiktokshop.com/docv2/page/search-coupon-list-202406) nói coupon được tạo ở Seller Center/App; chưa xác minh endpoint tạo coupon qua API phù hợp yêu cầu. Shopee Open Platform chưa truy cập được nội dung tài liệu chính thức liên quan trong lần kiểm tra này. Đây là **phụ thuộc cần xác minh** trước thiết kế tích hợp cuối cùng.

## Nguồn đọc lại

- Hai activity flow đã cập nhật: [SmartOmni_Additional_Flows_Detailed.md](SmartOmni_Additional_Flows_Detailed.md).
- Bối cảnh hệ thống cũ: [architecture.md](architecture.md), [erd.dbml](erd.dbml), [database-migrations.md](database-migrations.md); file mẫu người dùng đưa ở ngoài repo là `C:\Users\ASUS\Downloads\SmartOmni_All_Flows_Detailed.md`.
- API TikTok chính thức đã tham chiếu: [Get Conversation](https://partner.tiktokshop.com/docv2/page/get-conversation-202601), [Get Order List](https://partner.tiktokshop.com/docv2/page/get-order-list-202309), [Send Message](https://partner.tiktokshop.com/docv2/page/send-message), [Search Coupon List](https://partner.tiktokshop.com/docv2/page/search-coupon-list-202406).

## Câu hỏi còn mở trước khi triển khai

1. Tài khoản Customer Portal dùng email và số điện thoại theo cơ chế xác minh nào, và một số điện thoại có thể có tài khoản ở nhiều tenant hay không?
2. ID khách chat và ID người mua trong API Shopee có trường nào nối được chắc chắn? Với TikTok, `participants[].user_id` có đúng bằng `buyer_user_id` trong thị trường/shop dự kiến không?
3. App/shop cụ thể được cấp những scope API chat, order và promotion nào? Có endpoint **tạo voucher** cho Shopee/TikTok trong thị trường mục tiêu không?
4. Khi một mã đơn được claim bởi người không phải người mua thật, quy trình giải quyết tranh chấp là gì?
5. Nếu sàn ghi hoàn/trả sau 15 ngày hoặc sửa tiền khách thực trả, có ngoại lệ thu hồi điểm/hạng không?
6. Khi cùng tenant có nhiều Staff, có cần phân công chủ hội thoại hoặc chỉ cần cùng xem/cùng trả lời?
7. Khả năng AI phân loại tin đính kèm ảnh/video/tệp: phân loại chỉ trên văn bản hay có xử lý đa phương thức?
8. “5p0 tin/ngày” có đúng là **500 tin/ngày** không?

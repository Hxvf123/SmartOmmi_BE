# Quy trình làm việc với GitHub

Quy trình này áp dụng cho repository backend SmartOmni. Đọc thêm [quy ước đóng góp](../CONTRIBUTING.md) và [hướng dẫn chạy local](team-local-setup.md).

## Vai trò của các nhánh

| Nhánh | Mục đích |
|---|---|
| `main` | Mã có thể triển khai; chỉ nhận thay đổi qua Pull Request (PR) đã được review. |
| `develop` | Nhánh tích hợp của team; nhận PR từ các nhánh công việc. |
| `feature/<service>-<mo-ta-ngan>` | Phát triển một tính năng hoặc phần việc mới. |
| `fix/<service>-<mo-ta-ngan>` | Sửa một lỗi cụ thể. |

Luồng thông thường: `feature/*` hoặc `fix/*` → PR vào `develop` → PR phát hành từ `develop` vào `main`. Không đẩy commit trực tiếp lên `main` hoặc `develop`, và không force-push hai nhánh dùng chung này.

## Bắt đầu một công việc

Đồng bộ nhánh tích hợp và tạo nhánh riêng từ đó. Các lệnh chạy ở thư mục gốc repository:

```powershell
git status --short --branch
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/order-webhook-idempotency
```

Nếu mới clone và chưa có `develop` cục bộ, dùng `git switch --track origin/develop` thay cho `git switch develop`. Đổi tên nhánh ví dụ theo service và công việc thực tế. Nếu đang có thay đổi chưa commit, lưu hoặc hoàn tất chúng trước khi chuyển nhánh; không dùng lệnh xóa thay đổi chỉ để chuyển nhánh.

## Trong khi làm việc

- Giữ mỗi nhánh/PR tập trung vào một công việc. Đặt test tại `src/test/java`; kiểm tra trường hợp lỗi và cách ly tenant khi logic liên quan.
- Entity thuộc tenant dùng `BaseTenantEntity`; truy vấn phải lấy tenant từ `TenantContext`, không hardcode `tenant_id`.
- Thay đổi schema phải có migration mới trong `smartomni-db-migrations/src/main/resources/db/migration/`; không sửa migration đã áp dụng và giữ Hibernate `validate`.
- Cập nhật tài liệu tương ứng khi thay đổi API, kiến trúc, cấu hình hoặc quy trình chạy local.
- Không commit `.env`, secret, thông tin đăng nhập, log, database dump hoặc output build. Trước khi commit, kiểm tra các file được stage bằng `git diff --cached`.

Commit theo Conventional Commits, ví dụ:

```powershell
git add -- path/to/changed-file
git diff --cached
git commit -m "feat(order): add webhook idempotency"
```

Thay `path/to/changed-file` bằng đường dẫn file thực tế; có thể liệt kê nhiều file. Chỉ stage file thuộc công việc đang làm.

## Kiểm tra và mở PR

Trước khi mở PR, chạy từ thư mục gốc:

```powershell
mvn -B clean verify
git status --short --branch
git push -u origin feature/order-webhook-idempotency
```

Trên GitHub, tạo PR với **base `develop`** và **compare là nhánh công việc**. PR cần nêu: mục tiêu, thay đổi chính, cách kiểm thử/kết quả, ảnh hưởng tới API hoặc migration, và vấn đề còn lại nếu có. Gắn issue liên quan nếu team đang dùng Issues. Yêu cầu ít nhất một thành viên khác review; xử lý góp ý và chờ CI đạt trước khi merge. Workflow hiện tại chạy `mvn -B clean verify` cho PR vào `develop` và `main`.

Nếu `develop` đã thay đổi trong lúc review, cập nhật nhánh công việc, giải quyết conflict rồi chạy lại kiểm thử. Sau khi PR được merge, đồng bộ `develop` và xóa nhánh công việc đã hoàn tất. Không xóa nhánh khi PR còn mở.

## Đưa thay đổi lên `main`

Khi team quyết định phát hành, mở PR với **base `main`** và **compare `develop`**. Xem toàn bộ diff, kiểm tra CI, ghi nhận thay đổi và lấy review trước khi merge. Không dùng PR phát hành để đưa thêm thay đổi chưa qua `develop`. Sau merge, tiếp tục phát triển trên `develop` mới nhất.

## Cấu hình GitHub nên áp dụng

Người quản trị repository nên đặt branch protection/ruleset cho `main` và `develop`: yêu cầu PR, ít nhất một approval, CI `build` thành công và các hội thoại review được xử lý trước khi merge; chặn push trực tiếp và force-push. Quy định trong tài liệu chỉ được GitHub tự động thực thi sau khi các rule này được bật. Xem [GitHub Docs về protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).

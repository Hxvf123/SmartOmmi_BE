# Hướng dẫn cấu hình database local trên máy mới

Tài liệu dành cho thành viên chạy SmartOmni trên máy cá nhân bằng Docker Compose. Mỗi máy có database và dữ liệu riêng; không cần kết nối database trên máy của người khác. Các lệnh bên dưới chạy tại thư mục gốc repository, nơi có `docker-compose.yml`.

## 1. Chuẩn bị

- Lấy bản repository có `smartomni-db-migrations`, `scripts/db/` và `.env.example`.
- Cài Docker Desktop, mở ứng dụng và chờ Docker Engine sẵn sàng. Trên Windows, dùng backend WSL 2 và Linux containers.
- Có kết nối Internet để tải image và dependency trong lần build đầu.
- Nếu chạy hoàn toàn bằng Docker, không cần cài Java/Maven trên máy. Nếu chạy service bằng IDE, dùng JDK 17 hoặc 21 và Maven 3.9+. Đã kiểm tra build bằng JDK 21; Java 25 gây lỗi tương thích với dependency hiện tại.

Kiểm tra:

```powershell
docker version
docker compose version
```

`docker version` phải hiển thị được cả Client và Server. Nếu chưa nhận lệnh `docker`, mở lại terminal sau khi cài Docker Desktop.

## 2. Tạo `.env` riêng trên từng máy

Không copy `.env` chứa secret của thành viên khác. Git chỉ lưu `.env.example`; `.env` đã được bỏ qua bởi `.gitignore`.

Trên Windows, chạy đoạn PowerShell sau để tạo `.env` từ mẫu và thay cả 6 placeholder bằng secret ngẫu nhiên. Đoạn này dừng nếu `.env` đã tồn tại.

```powershell
if (Test-Path .env) {
    throw '.env đã tồn tại. Kiểm tra file hiện có trước khi tạo lại.'
}

$templateText = [IO.File]::ReadAllText((Join-Path (Get-Location) '.env.example'))
$randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    foreach ($keyName in @(
        'POSTGRES_PASSWORD',
        'DB_MIGRATION_PASSWORD',
        'DB_APP_PASSWORD',
        'RABBITMQ_PASSWORD',
        'JWT_SECRET',
        'SMARTOMNI_ENCRYPTION_AES_KEY'
    )) {
        $secretBytes = New-Object byte[] 32
        $randomGenerator.GetBytes($secretBytes)
        $secretValue = [Convert]::ToBase64String($secretBytes)
        $templateText = [regex]::Replace(
            $templateText,
            '(?m)^' + $keyName + '=.*$',
            $keyName + '=' + $secretValue
        )
    }
    [IO.File]::WriteAllText(
        (Join-Path (Get-Location) '.env'),
        $templateText,
        (New-Object Text.UTF8Encoding($false))
    )
} finally {
    $randomGenerator.Dispose()
}
```

Trên hệ điều hành khác, copy `.env.example` thành `.env` và thay tất cả placeholder bằng secret riêng. `JWT_SECRET` phải có ít nhất 32 byte; `SMARTOMNI_ENCRYPTION_AES_KEY` phải là Base64 của đúng 32 byte ngẫu nhiên.

| Biến trong `.env` | Dùng cho |
|---|---|
| `POSTGRES_PASSWORD` | Tài khoản quản trị `postgres` |
| `DB_MIGRATION_PASSWORD` | Tài khoản `smartomni_migrator`, chạy Flyway |
| `DB_APP_PASSWORD` | Tài khoản `smartomni_app`, được service dùng để truy vấn |
| `RABBITMQ_PASSWORD` | Mật khẩu RabbitMQ cho Order và Integration |
| `JWT_SECRET` | Secret JWT chung cho các service trên máy này |
| `SMARTOMNI_ENCRYPTION_AES_KEY` | Khóa mã hóa credential của Integration |

Kiểm tra cấu hình mà không in secret:

```powershell
docker compose config --quiet
git check-ignore .env
```

Lệnh đầu phải kết thúc không có lỗi; lệnh sau phải trả về `.env`.

## 3. Khởi động database và áp dụng migration

Chạy hạ tầng:

```powershell
docker compose up -d postgres redis rabbitmq
docker compose ps
```

PostgreSQL bootstrap tạo database `smartomni` và hai role `smartomni_migrator`, `smartomni_app` khi volume mới được khởi tạo. Bước này chưa tạo bảng nghiệp vụ.

Chạy toàn bộ hệ thống để các service tự áp dụng migration:

```powershell
docker compose up -d --build
docker compose logs -f auth-service
```

Flyway chạy trước Hibernate và áp dụng V1–V8. Các service dùng chung history, nên migration chỉ được áp dụng một lần. Nhấn `Ctrl+C` để dừng xem log; container vẫn chạy.

Build và khởi động lần đầu có thể mất vài phút. Chờ log `Started AuthServiceApplication` và kiểm tra schema ở bước tiếp theo. Không dùng `/actuator/health` để xác nhận trong cấu hình hiện tại vì endpoint này chưa được thiết lập cho toàn bộ service.

## 4. Kiểm tra database đã sẵn sàng

```powershell
docker compose ps -a
docker compose exec -T postgres psql -U postgres -d smartomni -c "SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;"
docker compose exec -T postgres psql -U postgres -d smartomni -c "SELECT count(*) AS business_tables FROM pg_tables WHERE schemaname='public' AND tablename <> 'flyway_schema_history';"
```

Với phiên bản repository hiện tại, kết quả mong đợi:

- 11 container đang chạy; PostgreSQL và RabbitMQ báo `healthy`.
- History có V1–V8, tất cả `success = t`.
- Có 44 bảng nghiệp vụ, không tính `flyway_schema_history`.
- Log các ứng dụng có `Started ...Application`, không có lỗi khởi động hoặc Hibernate validation.

Kiểm tra thêm cấu trúc schema bằng PowerShell:

```powershell
Get-Content -Raw scripts/db/verify-schema.sql | docker compose exec -T postgres psql -U postgres -d smartomni -v ON_ERROR_STOP=1
```

Kết quả thành công là `BEGIN`, `DO`, `COMMIT`, không có lỗi. Khi repository bổ sung migration mới, đối chiếu history với các file trong `smartomni-db-migrations/src/main/resources/db/migration/`.

## 5. Kết nối bằng DBeaver, pgAdmin hoặc IDE

| Thiết lập | Giá trị |
|---|---|
| Driver | PostgreSQL |
| Host | `localhost` |
| Port | `5432` |
| Database | `smartomni` |
| Username ứng dụng | `smartomni_app` |
| Password ứng dụng | Giá trị `DB_APP_PASSWORD` trong `.env` trên máy này |
| JDBC URL trên máy host | `jdbc:postgresql://localhost:5432/smartomni` |

Role ứng dụng chịu RLS: truy vấn thủ công không có tenant context có thể trả về rỗng hoặc bị từ chối. Role này cũng không được đọc Flyway history. Khi cần kiểm tra schema/history qua GUI, dùng `postgres` với `POSTGRES_PASSWORD`; service vẫn phải dùng `smartomni_app` và role migration riêng.

Trong Docker Compose, service dùng `jdbc:postgresql://postgres:5432/smartomni` vì `postgres` là tên service trong mạng Docker. Không đổi host này thành `localhost` trong container.

Nginx Gateway truy cập tại `http://localhost:8080`; RabbitMQ Management tại `http://localhost:15672` với tài khoản `smartomni` và mật khẩu `RABBITMQ_PASSWORD` trong `.env` của máy này. Đặt thêm `INTERNAL_JOB_TOKEN` (ít nhất 32 ký tự) trong `.env` cho Quartz worker Java; Python FastAPI chạy riêng và địa chỉ của nó có thể đặt bằng `PYTHON_AI_BASE_URL`.

Migration chỉ tạo schema, không seed tài khoản, tenant hoặc subscription plan. Việc database sẵn sàng không đồng nghĩa đã có tài khoản để đăng nhập.

## 6. Chạy service bằng IDE thay vì container

Giữ PostgreSQL, Redis, RabbitMQ chạy trong Docker. Nếu service tương ứng đang chạy bằng Compose, dừng nó trước để tránh trùng cổng, ví dụ:

```powershell
docker compose stop auth-service
mvn clean install -DskipTests
```

Trong Run Configuration của IDE, đặt các biến môi trường bằng giá trị từ `.env`:

- `DB_URL=jdbc:postgresql://localhost:5432/smartomni`
- `DB_APP_PASSWORD`: password của `smartomni_app`.
- `DB_MIGRATION_PASSWORD`: password của `smartomni_migrator`.
- `RABBITMQ_PASSWORD`: bắt buộc khi chạy Order hoặc Integration.
- `JWT_SECRET`: cùng giá trị với các service Docker trên máy này.
- `SMARTOMNI_ENCRYPTION_AES_KEY`: bắt buộc khi chạy Integration.

Spring Boot không tự đọc `.env`; phải cấu hình environment cho tiến trình Java. Service chạy ngoài Docker cần dùng địa chỉ `localhost` cho hạ tầng. Khi gọi service khác, kiểm tra các URL trong `application.yml` của module và dùng cổng local tương ứng.

## 7. Dừng, chạy lại và xử lý lỗi

```powershell
docker compose logs --tail 100 auth-service
docker compose down
docker compose up -d
```

`down` dừng và xóa container/network, nhưng giữ volume PostgreSQL. `up -d` chạy lại image đã build. Khi thay đổi code hoặc migration, dùng `docker compose up -d --build`.

| Hiện tượng | Cách xử lý |
|---|---|
| Không kết nối được Docker Engine / thiếu pipe `dockerDesktopLinuxEngine` | Mở Docker Desktop, chờ engine sẵn sàng rồi chạy lại `docker version`. |
| Compose báo thiếu biến hoặc placeholder chưa được thay | Kiểm tra `.env` tại thư mục gốc và chạy `docker compose config --quiet`. |
| Cổng bị chiếm | Kiểm tra ứng dụng/container đang dùng 5432, 6379, 5672, 15672 hoặc 8080–8087. Dừng tiến trình xung đột; nếu đổi cổng host PostgreSQL, cập nhật URL trong GUI/IDE. |
| `password authentication failed` sau khi sửa `.env` | Password trong volume cũ không tự đổi theo `.env`. Khôi phục giá trị khớp database hoặc đổi password role bằng admin rồi đồng bộ `.env`. |
| `role smartomni_app does not exist` / database có schema cũ | Bootstrap chỉ chạy trên volume mới. Làm theo quy trình database hiện có trong [database-migrations.md](database-migrations.md). |
| Chạy hạ tầng nhưng không thấy bảng | Khởi động ít nhất một service dùng database để Flyway chạy; bước 3 hướng dẫn chạy toàn bộ hệ thống. |
| Flyway báo checksum/schema khác biệt | Kiểm tra migration và database theo [database-migrations.md](database-migrations.md); không bật baseline tự động hoặc dùng repair để che lỗi. |

Không dùng `docker compose down -v` để xử lý lỗi thông thường: tùy chọn `-v` xóa volume và dữ liệu database. Không import bộ SQL cũ từ `Database/files` vào database mới dùng Flyway. Giữ Hibernate ở `validate`.

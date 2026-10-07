# Hướng dẫn cho thành viên mới: clone và chạy SmartOmni trên máy cá nhân

Tài liệu này dành cho người mới nhận repository backend SmartOmni. Mục tiêu của lần chạy đầu là: clone đúng mã nguồn, khởi động hạ tầng và các service Java, áp dụng migration lên database riêng trên máy, rồi kiểm tra Nginx Gateway. Các giao diện web và Python FastAPI AI nằm ngoài repository này; không cần chúng để xác nhận **khung backend** khởi động.

Các lệnh bên dưới chạy trong **thư mục gốc repository** (nơi có `docker-compose.yml`). Phần chính dùng PowerShell trên Windows. Trên macOS/Linux, các lệnh `git`, `docker compose` và `mvn` giữ nguyên; dùng `cp .env.example .env` thay cho `Copy-Item` và tự tạo secret như mục 2.

## 1. Cần chuẩn bị gì?

| Thành phần | Chạy toàn bộ bằng Docker | Chạy Java bằng IDE |
|---|---|---|
| Git và quyền đọc repository | Bắt buộc | Bắt buộc |
| Docker Desktop/Engine có Docker Compose v2 | Bắt buộc | Bắt buộc để chạy PostgreSQL, Redis, RabbitMQ theo hướng dẫn này |
| JDK 17 và Maven 3.9+ | Không cần cài trên host: Dockerfile tự dùng Maven/JDK 17 | Bắt buộc; JDK 21 đã được kiểm tra, tránh JDK 25 với dependency hiện tại |
| Internet lần đầu | Tải image và Maven dependencies | Tải image và Maven dependencies |

Trên Windows, mở Docker Desktop với **WSL 2 backend** và **Linux containers**. Đợi engine sẵn sàng rồi kiểm tra:

```powershell
git --version
docker version
docker compose version
```

`docker version` phải trả về cả Client và Server. Nếu chỉ thấy Client hoặc lỗi `dockerDesktopLinuxEngine`, mở/khởi động lại Docker Desktop trước khi tiếp tục. Đảm bảo các cổng host `8080`, `5432`, `6379`, `5672`, `15672` chưa bị ứng dụng khác chiếm. Các service Java trong Compose chỉ mở trong mạng Docker; host không cần dành cổng `8081`–`8087` trừ khi chạy chúng qua IDE.

## 2. Clone và tạo cấu hình riêng

```powershell
git clone https://github.com/Hxvf123/SmartOmmi_BE.git
cd SmartOmmi_BE
git status --short --branch
```

Nếu repository yêu cầu xác thực, dùng tài khoản GitHub đã được cấp quyền. Sau khi clone, checkout nhánh mà team thống nhất để phát triển; mặc định lệnh trên lấy nhánh mặc định. Không copy `.env` của thành viên khác hoặc commit secret lên Git.

Tạo `.env` từ `.env.example`. Đoạn PowerShell sau tạo **bảy** secret riêng bằng bộ sinh số ngẫu nhiên bảo mật, không in giá trị ra terminal và dừng nếu `.env` đã tồn tại. Đoạn này chạy được trên Windows PowerShell 5.1 và PowerShell 7:

```powershell
if (Test-Path .env) { throw '.env đã tồn tại; kiểm tra file hiện có trước khi tiếp tục.' }
$envText = [IO.File]::ReadAllText((Join-Path (Get-Location) '.env.example'))
$keys = @(
    'POSTGRES_PASSWORD',
    'DB_MIGRATION_PASSWORD',
    'DB_APP_PASSWORD',
    'RABBITMQ_PASSWORD',
    'JWT_SECRET',
    'SMARTOMNI_ENCRYPTION_AES_KEY',
    'INTERNAL_JOB_TOKEN'
)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    foreach ($key in $keys) {
        $bytes = New-Object byte[] 32
        $rng.GetBytes($bytes)
        $value = [Convert]::ToBase64String($bytes)
        $pattern = '(?m)^' + [regex]::Escape($key) + '=.*$'
        if (-not [regex]::IsMatch($envText, $pattern)) { throw "Thiếu $key trong .env.example" }
        $envText = [regex]::Replace($envText, $pattern, $key + '=' + $value)
    }
} finally {
    $rng.Dispose()
}
[IO.File]::WriteAllText((Join-Path (Get-Location) '.env'), $envText, (New-Object Text.UTF8Encoding($false)))
```

Nếu tạo thủ công: `Copy-Item .env.example .env`, rồi thay từng placeholder bằng chuỗi ngẫu nhiên riêng; `SMARTOMNI_ENCRYPTION_AES_KEY` phải là Base64 của **đúng 32 byte**. Trên macOS/Linux, `cp .env.example .env` rồi thay mọi `replace_with_...`; có thể tạo secret bằng `openssl rand -base64 32` (một giá trị mới cho mỗi biến). Không dán secret vào chat, issue hoặc PR.

| Biến trong `.env` | Vai trò |
|---|---|
| `POSTGRES_PASSWORD` | Mật khẩu tài khoản quản trị PostgreSQL trên máy này. |
| `DB_MIGRATION_PASSWORD` | Mật khẩu `smartomni_migrator` dùng bởi Flyway. |
| `DB_APP_PASSWORD` | Mật khẩu `smartomni_app` dùng bởi các service. |
| `RABBITMQ_PASSWORD` | Mật khẩu user `smartomni` của RabbitMQ. |
| `JWT_SECRET` | Secret ký JWT chung cho các Java service; tối thiểu 32 byte. |
| `SMARTOMNI_ENCRYPTION_AES_KEY` | Base64 của 32 byte, dùng để mã hóa credential trong Integration Service. |
| `INTERNAL_JOB_TOKEN` | Token ít nhất 32 ký tự để Quartz worker gọi các endpoint job nội bộ. |
| `PYTHON_AI_BASE_URL` | Tùy chọn; URL của Python FastAPI AI chạy ở repo riêng. Compose mặc định `http://host.docker.internal:8000`. |

Kiểm tra cấu hình **mà không in secret**:

```powershell
docker compose config --quiet
git check-ignore .env
```

Lệnh đầu thoát không lỗi; lệnh sau in `.env`. `.env` của mỗi máy độc lập; đổi password trong file sau khi PostgreSQL đã có volume **không tự đổi** password trong database.

## 3. Khởi động toàn bộ backend bằng Docker

```powershell
docker compose up -d --build
docker compose ps -a
```

Lần đầu Docker tải image và Maven tải dependencies trong lúc build, nên có thể mất vài phút. `--build` cần dùng lại sau khi đổi code hoặc Dockerfile; những lần khởi động lại không đổi code có thể dùng `docker compose up -d`. Compose khởi động 12 container: PostgreSQL, Redis, RabbitMQ, Nginx, bảy Java domain service và Quartz jobs service. `smartomni-gateway/` là mã Spring Gateway cũ, không thuộc stack hiện tại.

Theo dõi log nếu service chưa lên:

```powershell
docker compose logs --tail 100 auth-service
docker compose logs --tail 100 gateway
docker compose logs --tail 100 jobs-service
```

PostgreSQL tạo database `smartomni` và hai role ứng dụng/migration khi **volume mới** được khởi tạo. Flyway của các service áp dụng migration; Hibernate kiểm tra schema bằng `validate`. Không import SQL schema cũ bằng tay vào database mới.

## 4. Xác nhận chạy thành công

```powershell
docker compose ps -a
curl.exe -i http://localhost:8080/health
docker compose exec -T postgres psql -U postgres -d smartomni -c "SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;"
```

Kết quả mong đợi: các container đang `Up` (PostgreSQL, RabbitMQ và Nginx có healthcheck), `/health` trả HTTP `200` với nội dung `ok`, bảng Flyway có các migration thành công. Tại thời điểm viết tài liệu, migration là V1–V8; nếu team thêm migration, đối chiếu với thư mục `smartomni-db-migrations/src/main/resources/db/migration/`. Xem log `Started ...Application` của Java service khi cần xác nhận chi tiết. `/health` ở đây là **health của Nginx**, không chứng minh mọi luồng nghiệp vụ hay Python AI đã hoạt động. Không dùng `/actuator/health` làm tiêu chí chung vì chưa cấu hình cho toàn bộ service.

Các địa chỉ thường dùng:

| Địa chỉ | Mục đích |
|---|---|
| `http://localhost:8080` | Điểm vào API qua Nginx; `/health` để kiểm tra gateway. |
| `http://localhost:15672` | RabbitMQ Management; user `smartomni`, password từ `RABBITMQ_PASSWORD`. |
| `localhost:5432` | PostgreSQL `smartomni`; dùng DBeaver/IDE nếu cần. |

Nginx chỉ chuyển tiếp các path API/webhook được cấu hình trong `nginx/nginx.conf`; gọi `/` có thể trả `404` và đó không phải lỗi khởi động. Không có tài khoản tenant/super admin, subscription plan hoặc dữ liệu demo được seed bởi migration. Vì vậy **khởi động thành công không đồng nghĩa đăng nhập hoặc chạy hết nghiệp vụ ngay được**. Tài khoản Super Admin đầu tiên và dữ liệu khởi tạo phải được team provision riêng; luồng tự đăng ký Tenant Admin vẫn đang triển khai. Shopee/TikTok API, frontend và Python AI cũng chưa được cấp kèm trong repo này.

## 5. Nếu cần phát triển một service bằng IDE

Chỉ cần bước này khi muốn debug Java trên host. Cài JDK 17 (hoặc 21 đã kiểm tra) và Maven 3.9+, kiểm tra `java -version`, `mvn -version`. Đầu tiên chạy hạ tầng:

```powershell
docker compose up -d postgres redis rabbitmq
mvn clean install
```

`mvn clean install` build/test và cài module chung vào local Maven repository. Nếu stack Docker đã chạy, dừng container của service định debug trước (ví dụ `docker compose stop auth-service`), rồi chạy ứng dụng từ module tương ứng bằng IDE hoặc `mvn spring-boot:run` trong thư mục module. Compose không publish cổng của các Java service ra host. Khi chạy Java trên host, Nginx trong Docker vẫn trỏ đến container theo tên service; để thử service đang debug, gọi trực tiếp `http://localhost:<cổng service>` hoặc cấu hình lại gateway phù hợp.

Spring Boot chạy qua IDE **không tự đọc `.env` của Compose**. Đặt environment trong Run Configuration từ `.env` trên máy mình: `DB_URL=jdbc:postgresql://localhost:5432/smartomni`, `DB_APP_PASSWORD`, `DB_MIGRATION_PASSWORD`, `JWT_SECRET`; thêm `RABBITMQ_PASSWORD` cho Order/Integration, `SMARTOMNI_ENCRYPTION_AES_KEY` cho Integration, `SMARTOMNI_JOBS_TOKEN` cho Order/Inventory/Integration nếu kiểm thử endpoint job. Với AI có thể đặt `PYTHON_AI_BASE_URL=http://localhost:8000`. Redis/RabbitMQ mặc định dùng `localhost` khi Java chạy trên host. Nếu chạy nhiều service bằng IDE, kiểm tra URL gọi chéo trong `application.yml`.

| Service | Cổng khi chạy qua IDE |
|---|---:|
| Auth | 8081 |
| Tenant | 8082 |
| Catalog | 8083 |
| Inventory | 8084 |
| Order | 8085 |
| Integration | 8086 |
| AI (Java) | 8087 |

Để kiểm tra gần với CI, chạy `mvn -B clean verify` từ root. Migration tests dùng PostgreSQL 16 tạm riêng và `verify` còn khởi động thử JAR; đây không phải bài test end-to-end của RabbitMQ, Nginx, marketplace hoặc Python AI.

## 6. Dừng, cập nhật mã và xử lý lỗi thường gặp

```powershell
docker compose down
git pull
docker compose up -d --build
```

`docker compose down` giữ volume database. **Không thêm `-v`** trừ khi cả team chủ động muốn xóa dữ liệu local; `down -v` xóa volume PostgreSQL. Trước khi `git pull`, commit hoặc cất thay đổi đang làm theo quy trình team.

| Triệu chứng | Kiểm tra/cách xử lý |
|---|---|
| Docker Engine không kết nối | Mở Docker Desktop, chọn Linux containers và chờ `docker version` thấy Server. |
| Build không tải được image/dependency | Kiểm tra Internet, proxy/VPN và log build; chạy lại `docker compose up -d --build` sau khi kết nối ổn định. |
| Cổng host bị chiếm | Kiểm tra `8080`, `5432`, `6379`, `5672`, `15672`; dừng ứng dụng/container xung đột hoặc thống nhất đổi mapping trong Compose. |
| Compose báo thiếu biến | Kiểm tra `.env` ở root, đủ bảy secret; chạy `docker compose config --quiet`. |
| `password authentication failed` | Volume PostgreSQL cũ vẫn giữ password trước đó; đồng bộ `.env` với database hoặc đổi role password bằng admin. Không xóa volume để chữa lỗi thông thường. |
| `role smartomni_app does not exist`, schema/checksum khác | Volume đã có schema cũ hoặc bootstrap chưa chạy trên volume mới. Làm theo [database-migrations.md](database-migrations.md), không tự baseline/repair migration. |
| Service vừa `Up` rồi thoát | Xem `docker compose ps -a` và `docker compose logs --tail 200 <service>`; ưu tiên lỗi Flyway, Hibernate, password hoặc kết nối RabbitMQ/Redis. `depends_on: service_started` không bảo đảm service phụ thuộc đã sẵn sàng. |
| Gateway `/health` trả 200 nhưng API lỗi | Xem log service đích, kiểm tra route trong `nginx/nginx.conf`; health của Nginx chỉ kiểm tra Nginx. |
| AI gọi Python không được | Python FastAPI là service/repo riêng; cần chạy nó và đặt `PYTHON_AI_BASE_URL` phù hợp. Backend Java có thể khởi động trước khi Python AI sẵn sàng. |

Tài liệu liên quan: [README hệ thống](../README.md), [thiết lập database local](local-database-setup.md), [database migrations](database-migrations.md), [kiến trúc](architecture.md) và [quy trình đóng góp](../CONTRIBUTING.md).

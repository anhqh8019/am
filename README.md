# Apartment Management

Hệ thống quản lý chung cư cho 2 tòa, hơn 400 căn hộ, bãi xe TTZ và mặt bằng kinh doanh.

## Công nghệ

- Java 25, Spring Boot 4.0.8, Maven
- React 19, TypeScript, Vite
- Microsoft SQL Server 2022
- Flyway, Spring Data JPA, Spring Security

## Khởi động môi trường phát triển

1. Sao chép `.env.example` thành `.env` và đổi mật khẩu.
2. Chạy SQL Server: `docker compose up -d sqlserver sqlserver-init`.
3. Backend: `cd backend && mvn spring-boot:run` hoặc `docker compose up -d backend`.
4. Frontend: `cd frontend && npm install && npm run dev`.

API mặc định: `http://localhost:8080/api`; frontend: `http://localhost:5173`.

## Đăng nhập ban đầu

Khi backend khởi động lần đầu, hệ thống tạo tài khoản quản trị từ các biến:

- `INITIAL_ADMIN_USERNAME`
- `INITIAL_ADMIN_PASSWORD` (tối thiểu 12 ký tự)
- `INITIAL_ADMIN_DISPLAY_NAME`

Giá trị mẫu trong `.env.example` chỉ dùng để phát triển. Phải đổi mật khẩu quản trị và `JWT_SECRET` trước khi triển khai. API đăng nhập là `POST /api/auth/login`; JWT mặc định có hiệu lực 8 giờ. Frontend lưu token trong `sessionStorage`, hoặc `localStorage` khi người dùng chọn ghi nhớ đăng nhập.

## Nguyên tắc tích hợp TTZ

Database `TTZ_P` là nguồn ngoài và chỉ được truy cập bằng tài khoản `SELECT`. Không ghi trực tiếp vào bảng TTZ khi chưa có tài liệu tích hợp hoặc xác nhận của nhà cung cấp.
"# am" 

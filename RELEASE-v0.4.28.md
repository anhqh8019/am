# Phiên bản v0.4.28 – CORS theo địa chỉ triển khai

- `SecurityConfig` sử dụng thật sự giá trị `app.security.allowed-origins`.
- Cho phép cấu hình nhiều origin bằng biến môi trường `ALLOWED_ORIGINS`, phân cách bằng dấu phẩy.
- IP LAN được khai báo khi triển khai qua biến môi trường, ví dụ `ALLOWED_ORIGINS=http://localhost:8088,http://192.168.1.222:8088`.
- Khắc phục đăng nhập trả về HTTP 403 khi truy cập giao diện bằng IP LAN.

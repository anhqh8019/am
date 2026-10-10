# Phiên bản v0.4.19 – Sửa lỗi 403 khi chạy qua Nginx

- Cho phép request `OPTIONS` phục vụ CORS preflight trước khi đăng nhập.
- Hỗ trợ origin của Nginx theo hostname hoặc địa chỉ IP máy chủ tại cổng `8088`.
- Nginx chuyển tiếp đầy đủ hostname và port đến Spring Boot.
- Giữ nguyên cơ chế JWT và phân quyền cho các API nghiệp vụ.

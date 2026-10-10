# Phiên bản v0.4.20 – Hoàn thiện xử lý CORS khi đăng nhập

- Cho phép frontend gọi API qua `localhost`, `127.0.0.1` và tên máy chủ triển khai.
- Không còn phụ thuộc biến `ALLOWED_ORIGINS` để thực hiện đăng nhập.
- Các API nghiệp vụ vẫn bắt buộc JWT và giữ nguyên phân quyền.

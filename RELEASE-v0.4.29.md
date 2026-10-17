# Phiên bản v0.4.29 – URL callback tương thích VietQR

- Public `POST /vietqr/api/token_generate` theo mẫu cấu hình VietQR.
- Public `POST /vietqr/bank/api/transaction-sync` theo mẫu cấu hình VietQR.
- Giữ lại hai URL `/api/vietqr/...` để tương thích với bản triển khai trước.
- Khắc phục HTTP 401 khi VietQR gọi Transaction Sync qua URL chuẩn đã đăng ký.

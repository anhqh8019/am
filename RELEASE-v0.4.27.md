# Phiên bản v0.4.27 – VietQR Callback APIs

- Public API cấp Bearer token cho VietQR bằng Basic Authentication.
- Public API Transaction Sync nhận biến động số dư từ VietQR.
- Đối chiếu theo `orderId`, tài khoản nhận và số tiền.
- Tự động tạo phiếu thu, phân bổ vào từng dịch vụ và cập nhật công nợ.
- Chống xử lý trùng bằng `transactionid` ngân hàng.

## Endpoint

- `POST /api/vietqr/token_generate`
- `POST /api/vietqr/transaction-sync`

## Biến môi trường bắt buộc khi triển khai

- `VIETQR_CALLBACK_USERNAME`
- `VIETQR_CALLBACK_PASSWORD`
- `VIETQR_CALLBACK_JWT_SECRET` (tối thiểu 32 byte)

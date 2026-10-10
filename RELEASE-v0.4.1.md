# Apartment Management v0.4.1

Ngày đóng gói: 03/10/2026

## Nội dung chính

- Thêm sửa và xóa/ngừng dịch vụ gửi xe trong hồ sơ căn hộ.
- Hiển thị nút chữ `Sửa` và `Xóa` tại từng dịch vụ không bắt buộc.
- Thêm sửa và xóa biểu phí dịch vụ tòa nhà.
- Cho phép tính lại công nợ chưa thanh toán sau khi thay đổi biểu phí hoặc dịch vụ.
- Giữ nguyên khoản công nợ đã thanh toán hoặc đã thanh toán một phần.
- Hiển thị `Phiên bản v0.4.1` ở cuối thanh menu bên trái.

## Cách xác nhận đúng phiên bản

Sau khi đăng nhập, cuối menu bên trái phải xuất hiện dòng `Phiên bản v0.4.1`.
Trong hồ sơ căn hộ, mỗi dịch vụ gửi xe phải có hai nút `Sửa` và `Xóa`.

## Build frontend

```powershell
cd frontend
npm install
npm run build
npm run dev
```

Sau khi khởi động lại, nhấn `Ctrl + Shift + R` trên trình duyệt.

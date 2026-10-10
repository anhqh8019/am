# Phiên bản v0.4.25 – Xử lý đúng phiên đăng nhập hết hạn

- JWT thiếu, sai hoặc hết hạn trả về HTTP 401 thay vì 403.
- Frontend tự xóa phiên đăng nhập cũ, tải lại màn hình và yêu cầu đăng nhập lại.
- HTTP 403 chỉ còn dùng cho trường hợp tài khoản đã đăng nhập nhưng không có quyền.

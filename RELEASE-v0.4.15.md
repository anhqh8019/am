# Phiên bản v0.4.15 – Bảo toàn lịch sử QR khi tính lại kỳ thu

- Khắc phục lỗi khóa ngoại khi tạo/cập nhật kỳ thu có khoản phải thu đã phát hành QR.
- Khoản đã thanh toán hoặc đã phát hành QR được giữ nguyên để bảo toàn lịch sử đối soát.
- Các khoản chưa thanh toán và chưa có QR vẫn được tính lại bình thường.
- Việc sinh khoản phải thu mới cho hợp đồng mặt bằng vẫn tiếp tục, không bị rollback toàn bộ giao dịch.

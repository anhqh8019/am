# Cấu hình VietQR Callback

## URL cung cấp cho VietQR

Thay `https://billing.example.vn` bằng tên miền thực tế:

- Get Token: `https://billing.example.vn/api/vietqr/token_generate`
- Transaction Sync: `https://billing.example.vn/api/vietqr/transaction-sync`

Hai URL phải truy cập được từ Internet bằng HTTPS với chứng thư hợp lệ.

## Biến môi trường WinSW

```xml
<env name="VIETQR_CALLBACK_USERNAME" value="vietqr-apartment" />
<env name="VIETQR_CALLBACK_PASSWORD" value="REPLACE_WITH_A_STRONG_RANDOM_PASSWORD" />
<env name="VIETQR_CALLBACK_JWT_SECRET" value="REPLACE_WITH_A_RANDOM_SECRET_AT_LEAST_32_BYTES" />
<env name="VIETQR_CALLBACK_TOKEN_EXPIRATION_SECONDS" value="300" />
```

Không dùng lại tài khoản quản trị phần mềm cho VietQR.

## Nginx

Nếu Nginx đã proxy toàn bộ `/api/` vào Spring Boot thì không cần thêm location mới:

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## Kiểm tra bằng PowerShell

```powershell
$baseUrl = "https://billing.example.vn"
$username = "vietqr-apartment"
$password = "REPLACE_WITH_A_STRONG_RANDOM_PASSWORD"
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${username}:${password}"))

$tokenResult = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/vietqr/token_generate" `
  -Headers @{ Authorization = "Basic $basic" } `
  -ContentType "application/json"

$tokenResult
```

Lấy một `referenceCode` của QR đang ở trạng thái `PENDING`, rồi kiểm tra callback:

```powershell
$orderId = "REPLACE_WITH_PENDING_REFERENCE_CODE"
$transactionId = "TEST-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$body = @{
  bankaccount     = "REPLACE_WITH_PAYMENT_BANK_ACCOUNT"
  amount          = 100000
  transType       = "C"
  content         = $orderId
  transactionid   = $transactionId
  transactiontime = $timestamp
  referencenumber = $transactionId
  orderId         = $orderId
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/vietqr/transaction-sync" `
  -Headers @{ Authorization = "Bearer $($tokenResult.access_token)" } `
  -ContentType "application/json" `
  -Body $body
```

Số tiền test phải đúng bằng tổng tiền của QR. Callback thành công sẽ tạo phiếu thu và chuyển công nợ tương ứng sang đã thanh toán; vì vậy chỉ test trên dữ liệu thử nghiệm.

package vn.com.apartment.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class VietQrApiClient {
    private final RestClient client;
    private final String username;
    private final String password;
    private String accessToken;
    private Instant refreshAt = Instant.EPOCH;

    public VietQrApiClient(@Value("${app.vietqr.client.base-url}") String baseUrl,
            @Value("${app.vietqr.client.username:}") String username,
            @Value("${app.vietqr.client.password:}") String password) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.username = username;
        this.password = password;
    }

    public GenerateResponse generateDynamicQr(String bankCode, String bankAccount,
            String accountName, BigDecimal amount, String orderId) {
        if (username.isBlank() || password.isBlank())
            throw new VietQrApiException("Chưa cấu hình VIETQR_API_USERNAME và VIETQR_API_PASSWORD.");
        try {
            GenerateRequest body = new GenerateRequest(normalizeBankCode(bankCode), bankAccount,
                accountName, orderId, 0, amount.longValueExact(), orderId, "C", null,
                null, null, null, null, "Thanh toan phi chung cu", List.of());
            GenerateResponse response = client.post().uri("/vqr/api/qr/generate-customer")
                .contentType(MediaType.APPLICATION_JSON).headers(h -> h.setBearerAuth(token()))
                .body(body).retrieve().body(GenerateResponse.class);
            if (response == null || response.qrCode() == null || response.qrCode().isBlank())
                throw new VietQrApiException("VietQR không trả về nội dung QR.");
            return response;
        } catch (RestClientResponseException ex) {
            throw new VietQrApiException("VietQR từ chối tạo QR (HTTP " + ex.getStatusCode().value()
                + "): " + ex.getResponseBodyAsString(), ex);
        } catch (ArithmeticException ex) {
            throw new VietQrApiException("Số tiền tạo QR phải là số nguyên đồng.", ex);
        }
    }

    private synchronized String token() {
        if (accessToken != null && Instant.now().isBefore(refreshAt)) return accessToken;
        TokenResponse response = client.post().uri("/vqr/api/token_generate")
            .headers(h -> h.setBasicAuth(username, password)).retrieve().body(TokenResponse.class);
        if (response == null || response.access_token() == null || response.access_token().isBlank())
            throw new VietQrApiException("VietQR không trả về access_token.");
        accessToken = response.access_token();
        refreshAt = Instant.now().plusSeconds(Math.max(30, response.expires_in() - 30));
        return accessToken;
    }

    private String normalizeBankCode(String value) {
        return "970422".equals(value) ? "MB" : value;
    }

    private record TokenResponse(String access_token, String token_type, long expires_in) {}
    private record GenerateRequest(String bankCode, String bankAccount, String userBankName,
        String content, int qrType, long amount, String orderId, String transType,
        String terminalCode, String subTerminalCode, String serviceCode, String sign,
        String urlLink, String note, List<Object> additionalData) {}
    public record GenerateResponse(String bankCode, String bankName, String bankAccount,
        String userBankName, String amount, String content, String qrCode, String imgId,
        Integer existing, String transactionId, String transactionRefId, String qrLink,
        String terminalCode, String subTerminalCode, String serviceCode, String orderId,
        List<Object> additionalData, String vaAccount) {}
    public static class VietQrApiException extends RuntimeException {
        public VietQrApiException(String message) { super(message); }
        public VietQrApiException(String message, Throwable cause) { super(message, cause); }
    }
}

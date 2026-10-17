package vn.com.apartment.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class VietQrCallbackController {
    private final VietQrCallbackTokenService tokens;
    private final BankPaymentReconciliationService reconciliation;
    private final String callbackUsername;
    private final String callbackPassword;

    public VietQrCallbackController(VietQrCallbackTokenService tokens,
            BankPaymentReconciliationService reconciliation,
            @Value("${app.vietqr.callback.username}") String callbackUsername,
            @Value("${app.vietqr.callback.password}") String callbackPassword) {
        this.tokens = tokens;
        this.reconciliation = reconciliation;
        this.callbackUsername = callbackUsername;
        this.callbackPassword = callbackPassword;
    }

    @PostMapping({"/api/vietqr/token_generate", "/vietqr/api/token_generate"})
    public ResponseEntity<?> generateToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Credentials credentials = decodeBasic(authorization);
        if (credentials == null)
            return ResponseEntity.badRequest().body(new FailedResponse("FAILED", "INVALID_AUTH_HEADER"));
        if (!constantEquals(callbackUsername, credentials.username())
                || !constantEquals(callbackPassword, credentials.password()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new FailedResponse("FAILED", "INVALID_CREDENTIALS"));
        return ResponseEntity.ok(new TokenResponse(tokens.create(credentials.username()),
            "Bearer", tokens.expirationSeconds()));
    }

    @PostMapping({"/api/vietqr/transaction-sync", "/vietqr/bank/api/transaction-sync"})
    public ResponseEntity<SyncResponse> transactionSync(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody TransactionSyncRequest request) {
        String token = bearer(authorization);
        if (token == null)
            return error(HttpStatus.UNAUTHORIZED, "INVALID_AUTH_HEADER", "Authorization header không hợp lệ.");
        if (!tokens.isValid(token))
            return error(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Bearer token không hợp lệ hoặc đã hết hạn.");
        if (!"C".equalsIgnoreCase(request.transType()))
            return error(HttpStatus.BAD_REQUEST, "INVALID_TRANS_TYPE", "Chỉ tiếp nhận giao dịch ghi có (C).");

        LocalDateTime transactionTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(request.transactiontime()), ZoneOffset.UTC);
        BankPaymentReconciliationService.Result result = reconciliation.reconcile(
            "VIETQR", request.transactionid(), request.bankaccount(), request.amount(),
            request.content(), transactionTime, request.orderId(), request.toString());
        return ResponseEntity.ok(new SyncResponse(false, null, result.message(),
            new SyncObject(request.transactionid())));
    }

    private ResponseEntity<SyncResponse> error(HttpStatus status, String reason, String message) {
        return ResponseEntity.status(status).body(new SyncResponse(true, reason, message, null));
    }

    private Credentials decodeBasic(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            return separator < 1 ? null : new Credentials(decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch (IllegalArgumentException ex) { return null; }
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
            ? authorization.substring(7).trim() : null;
    }

    private boolean constantEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private record Credentials(String username, String password) {}
    public record TokenResponse(String access_token, String token_type, long expires_in) {}
    public record FailedResponse(String status, String message) {}
    public record TransactionSyncRequest(
        @NotBlank String bankaccount,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[CDcd]") String transType,
        @NotBlank String content,
        @NotBlank String transactionid,
        @NotNull @Positive Long transactiontime,
        @NotBlank String referencenumber,
        @NotBlank String orderId,
        String terminalCode,
        String subTerminalCode,
        String serviceCode,
        String urlLink,
        String sign) {}
    public record SyncResponse(boolean error, String errorReason, String toastMessage, SyncObject object) {}
    public record SyncObject(String reftransactionid) {}
}

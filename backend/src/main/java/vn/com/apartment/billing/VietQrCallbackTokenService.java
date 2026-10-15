package vn.com.apartment.billing;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VietQrCallbackTokenService {
    private static final String ISSUER = "apartment-management";
    private static final String SCOPE = "vietqr:transaction-sync";
    private final SecretKey key;
    private final long expirationSeconds;

    public VietQrCallbackTokenService(
            @Value("${app.vietqr.callback.jwt-secret}") String secret,
            @Value("${app.vietqr.callback.token-expiration-seconds:300}") long expirationSeconds) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("VIETQR_CALLBACK_JWT_SECRET phải có ít nhất 32 byte");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String create(String username) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(ISSUER).subject(username).claim("scope", SCOPE)
            .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(key).compact();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                .parseSignedClaims(token).getPayload();
            return SCOPE.equals(claims.get("scope", String.class))
                && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long expirationSeconds() { return expirationSeconds; }
}

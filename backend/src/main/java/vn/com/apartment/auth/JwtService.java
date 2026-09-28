package vn.com.apartment.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;
    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.jwt-expiration-seconds:28800}") long expirationSeconds) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET phải có ít nhất 32 byte");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }
    public String create(UserDetails user) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Jwts.builder().subject(user.getUsername()).claim("roles", roles).issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
    }
    public String extractUsername(String token) { return parse(token).getPayload().getSubject(); }
    public boolean isValid(String token, UserDetails user) {
        try {
            Claims claims = parse(token).getPayload();
            return claims.getSubject().equalsIgnoreCase(user.getUsername()) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) { return false; }
    }
    public long getExpirationSeconds() { return expirationSeconds; }
    private Jws<Claims> parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token); }
}

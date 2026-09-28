package vn.com.apartment.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.com.apartment.identity.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService userDetailsService;
    private final UserRepository users;
    private final JwtService jwt;
    public AuthController(AuthenticationManager authenticationManager, AppUserDetailsService userDetailsService,
                          UserRepository users, JwtService jwt) {
        this.authenticationManager = authenticationManager; this.userDetailsService = userDetailsService; this.users = users; this.jwt = jwt;
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try { authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password())); }
        catch (AuthenticationException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không đúng"); }
        AppUser user = users.findByUsernameIgnoreCase(request.username()).orElseThrow();
        user.markLogin(); users.save(user);
        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        List<String> roles = details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new LoginResponse(jwt.create(details), "Bearer", jwt.getExpirationSeconds(),
            new UserInfo(user.getId(), user.getUsername(), user.getDisplayName(), roles, user.isPasswordChangeRequired()));
    }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserInfo user) {}
    public record UserInfo(Long id, String username, String displayName, List<String> roles, boolean passwordChangeRequired) {}
}

package vn.com.apartment.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminInitializer implements ApplicationRunner {
    private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder;
    private final String username; private final String password; private final String displayName;
    public InitialAdminInitializer(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
        @Value("${app.initial-admin.username}") String username, @Value("${app.initial-admin.password}") String password,
        @Value("${app.initial-admin.display-name:Quản trị hệ thống}") String displayName) {
        this.users=users; this.roles=roles; this.encoder=encoder; this.username=username; this.password=password; this.displayName=displayName;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (users.existsByUsernameIgnoreCase(username)) return;
        if (password.length() < 12) throw new IllegalStateException("INITIAL_ADMIN_PASSWORD phải có ít nhất 12 ký tự");
        Role admin = roles.findByCode("ADMIN").orElseThrow(() -> new IllegalStateException("Thiếu vai trò ADMIN"));
        users.save(new AppUser(username, encoder.encode(password), displayName, null, admin));
    }
}

package vn.com.apartment.identity;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public AppUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = users.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));
        String[] authorities = user.getRoles().stream().map(role -> "ROLE_" + role.getCode()).toArray(String[]::new);
        return User.withUsername(user.getUsername()).password(user.getPasswordHash())
            .authorities(authorities).disabled(!user.isEnabled()).build();
    }
}

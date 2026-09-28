package vn.com.apartment.identity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;
    @Column(length = 150)
    private String email;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired = true;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected AppUser() {}
    public AppUser(String username, String passwordHash, String displayName, String email, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.email = email;
        this.roles.add(role);
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public boolean isEnabled() { return enabled; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public Set<Role> getRoles() { return roles; }
    public void markLogin() { this.lastLoginAt = LocalDateTime.now(); }
}

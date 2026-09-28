package vn.com.apartment.building;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "buildings")
public class Building {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Building() {}
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

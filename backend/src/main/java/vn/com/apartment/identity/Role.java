package vn.com.apartment.identity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;

    protected Role() {}
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}

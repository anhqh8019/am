package vn.com.apartment.building;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "units", uniqueConstraints = @UniqueConstraint(name = "uq_unit", columnNames = {"building_id", "code"}))
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 20)
    private UnitType unitType;

    @Column(name = "area_m2", precision = 12, scale = 2)
    private BigDecimal areaM2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitStatus status = UnitStatus.ACTIVE;

    protected Unit() {}

    public Unit(Building building, String code, UnitType unitType, BigDecimal areaM2) {
        this.building = building;
        this.code = code;
        this.unitType = unitType;
        this.areaM2 = areaM2;
    }

    public Long getId() { return id; }
    public Building getBuilding() { return building; }
    public String getCode() { return code; }
    public UnitType getUnitType() { return unitType; }
    public BigDecimal getAreaM2() { return areaM2; }
    public UnitStatus getStatus() { return status; }

    public void update(Building building, String code, UnitType unitType, BigDecimal areaM2, UnitStatus status) {
        this.building = building;
        this.code = code;
        this.unitType = unitType;
        this.areaM2 = areaM2;
        this.status = status;
    }

    public void deactivate() { this.status = UnitStatus.INACTIVE; }
}

package vn.com.apartment.commercial;

import jakarta.persistence.*;
import java.math.BigDecimal;
import vn.com.apartment.building.Building;

@Entity
@Table(name = "commercial_spaces", uniqueConstraints = @UniqueConstraint(name = "uq_commercial_space", columnNames = {"building_id", "code"}))
public class CommercialSpace {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "building_id", nullable = false)
    private Building building;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false, length = 200) private String location;
    @Column(length = 500) private String description;
    @Column(name = "total_area_m2", nullable = false, precision = 12, scale = 2) private BigDecimal totalAreaM2;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CommercialSpaceStatus status = CommercialSpaceStatus.AVAILABLE;

    protected CommercialSpace() {}
    public CommercialSpace(Building building, String code, String location, String description, BigDecimal totalAreaM2) {
        update(building, code, location, description, totalAreaM2, CommercialSpaceStatus.AVAILABLE);
    }
    public void update(Building building, String code, String location, String description, BigDecimal totalAreaM2, CommercialSpaceStatus status) {
        this.building=building; this.code=code; this.location=location; this.description=description; this.totalAreaM2=totalAreaM2; this.status=status;
    }
    public void deactivate() { status = CommercialSpaceStatus.INACTIVE; }
    public Long getId(){return id;} public Building getBuilding(){return building;} public String getCode(){return code;}
    public String getLocation(){return location;} public String getDescription(){return description;}
    public BigDecimal getTotalAreaM2(){return totalAreaM2;} public CommercialSpaceStatus getStatus(){return status;}
}

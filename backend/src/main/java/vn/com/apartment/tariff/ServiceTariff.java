package vn.com.apartment.tariff;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import vn.com.apartment.building.Building;
import vn.com.apartment.unitprofile.ChargeMethod;

@Entity
@Table(name = "service_tariffs")
public class ServiceTariff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "service_code", nullable = false, length = 30)
    private String serviceCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 20)
    private ChargeMethod calculationType;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ServiceTariff() {}

    public ServiceTariff(String serviceCode, Building building, BigDecimal unitPrice,
                         ChargeMethod calculationType, LocalDate effectiveFrom,
                         LocalDate effectiveTo, String description) {
        this.serviceCode = serviceCode;
        this.building = building;
        this.unitPrice = unitPrice;
        this.calculationType = calculationType;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.description = description;
    }

    public void close(LocalDate date) {
        effectiveTo = date;
        active = false;
    }

    public Long getId() { return id; }
    public String getServiceCode() { return serviceCode; }
    public Building getBuilding() { return building; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public ChargeMethod getCalculationType() { return calculationType; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

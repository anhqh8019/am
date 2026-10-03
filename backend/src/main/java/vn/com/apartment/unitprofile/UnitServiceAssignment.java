package vn.com.apartment.unitprofile;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import vn.com.apartment.building.Unit;
import vn.com.apartment.tariff.ServiceTariff;

@Entity @Table(name="unit_service_assignments")
public class UnitServiceAssignment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="unit_id") private Unit unit;
    @Column(name="service_code",nullable=false,length=30) private String serviceCode;
    @Column(name="service_name",nullable=false,length=150) private String serviceName;
    @Column(length=100) private String variant;
    @Enumerated(EnumType.STRING) @Column(name="charge_method",nullable=false,length=20) private ChargeMethod chargeMethod;
    @Column(nullable=false,precision=18,scale=3) private BigDecimal quantity;
    @Column(name="unit_price",nullable=false,precision=18,scale=2) private BigDecimal unitPrice;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tariff_id") private ServiceTariff tariff;
    @Enumerated(EnumType.STRING) @Column(name="collection_mode",nullable=false,length=30) private CollectionMode collectionMode;
    @Column(name="license_plate",length=20) private String licensePlate;
    @Column(name="electric_vehicle",nullable=false) private boolean electricVehicle;
    @Column(nullable=false) private boolean mandatory;
    @Column(nullable=false) private boolean active=true;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    protected UnitServiceAssignment(){}
    public UnitServiceAssignment(Unit unit,String code,String name,String variant,ChargeMethod method,BigDecimal quantity,BigDecimal price,CollectionMode mode,boolean mandatory,LocalDate start){this(unit,code,name,variant,method,quantity,price,mode,mandatory,start,null,false);}
    public UnitServiceAssignment(Unit unit,String code,String name,String variant,ChargeMethod method,BigDecimal quantity,BigDecimal price,CollectionMode mode,boolean mandatory,LocalDate start,String plate,boolean electric){this.unit=unit;serviceCode=code;serviceName=name;this.variant=variant;chargeMethod=method;this.quantity=quantity;unitPrice=price;collectionMode=mode;this.mandatory=mandatory;startDate=start;licensePlate=plate;electricVehicle=electric;}
    public void end(LocalDate date){if(mandatory)throw new IllegalStateException("Không thể ngừng dịch vụ bắt buộc");active=false;endDate=date;}
    public void assignTariff(ServiceTariff tariff){this.tariff=tariff;}
    public Long getId(){return id;} public Unit getUnit(){return unit;} public String getServiceCode(){return serviceCode;} public String getServiceName(){return serviceName;} public String getVariant(){return variant;} public ChargeMethod getChargeMethod(){return chargeMethod;} public BigDecimal getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;} public ServiceTariff getTariff(){return tariff;} public CollectionMode getCollectionMode(){return collectionMode;} public String getLicensePlate(){return licensePlate;} public boolean isElectricVehicle(){return electricVehicle;} public boolean isMandatory(){return mandatory;} public boolean isActive(){return active;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;}
}

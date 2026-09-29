package vn.com.apartment.commercial;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="electricity_meters")
public class ElectricityMeter {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="commercial_space_id") private CommercialSpace commercialSpace;
    @Column(name="meter_code",nullable=false,unique=true,length=50) private String meterCode;
    @Column(length=200) private String description;
    @Column(name="default_unit_price",nullable=false,precision=18,scale=2) private BigDecimal defaultUnitPrice;
    @Column(nullable=false) private boolean active=true;
    protected ElectricityMeter() {}
    public ElectricityMeter(CommercialSpace space,String code,String description,BigDecimal price){commercialSpace=space;meterCode=code;this.description=description;defaultUnitPrice=price;}
    public Long getId(){return id;} public CommercialSpace getCommercialSpace(){return commercialSpace;} public String getMeterCode(){return meterCode;}
    public String getDescription(){return description;} public BigDecimal getDefaultUnitPrice(){return defaultUnitPrice;} public boolean isActive(){return active;}
}

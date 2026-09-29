package vn.com.apartment.commercial;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity @Table(name="lease_contracts")
public class LeaseContract {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="contract_number",nullable=false,unique=true,length=50) private String contractNumber;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="commercial_space_id") private CommercialSpace commercialSpace;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private CommercialTenant tenant;
    @Column(name="leased_area_m2",nullable=false,precision=12,scale=2) private BigDecimal leasedAreaM2;
    @Column(name="unit_price_per_m2",nullable=false,precision=18,scale=2) private BigDecimal unitPricePerM2;
    @Column(name="vat_rate",nullable=false,precision=5,scale=2) private BigDecimal vatRate;
    @Column(name="deposit_amount",nullable=false,precision=18,scale=2) private BigDecimal depositAmount;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false) private LocalDate endDate;
    @Column(name="payment_day",nullable=false) private int paymentDay;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private LeaseStatus status;
    @Column(length=500) private String notes;
    protected LeaseContract() {}
    public LeaseContract(String number,CommercialSpace space,CommercialTenant tenant,BigDecimal area,BigDecimal price,BigDecimal vat,BigDecimal deposit,LocalDate start,LocalDate end,int paymentDay,LeaseStatus status,String notes){
        contractNumber=number;commercialSpace=space;this.tenant=tenant;leasedAreaM2=area;unitPricePerM2=price;vatRate=vat;depositAmount=deposit;startDate=start;endDate=end;this.paymentDay=paymentDay;this.status=status;this.notes=notes;
    }
    public BigDecimal getSubtotal(){return leasedAreaM2.multiply(unitPricePerM2).setScale(2,RoundingMode.HALF_UP);}
    public BigDecimal getVatAmount(){return getSubtotal().multiply(vatRate).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);}
    public BigDecimal getMonthlyTotal(){return getSubtotal().add(getVatAmount());}
    public Long getId(){return id;} public String getContractNumber(){return contractNumber;} public CommercialSpace getCommercialSpace(){return commercialSpace;}
    public CommercialTenant getTenant(){return tenant;} public BigDecimal getLeasedAreaM2(){return leasedAreaM2;} public BigDecimal getUnitPricePerM2(){return unitPricePerM2;}
    public BigDecimal getVatRate(){return vatRate;} public BigDecimal getDepositAmount(){return depositAmount;} public LocalDate getStartDate(){return startDate;}
    public LocalDate getEndDate(){return endDate;} public int getPaymentDay(){return paymentDay;} public LeaseStatus getStatus(){return status;} public String getNotes(){return notes;}
}

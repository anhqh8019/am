package vn.com.apartment.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import vn.com.apartment.building.Unit;
import vn.com.apartment.resident.Customer;
import vn.com.apartment.commercial.*;

@Entity @Table(name="receivables")
public class Receivable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="reference_code",nullable=false,unique=true,length=50) private String referenceCode;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id") private Customer customer;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="unit_id") private Unit unit;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="commercial_tenant_id") private CommercialTenant commercialTenant;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="commercial_space_id") private CommercialSpace commercialSpace;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="lease_contract_id") private LeaseContract leaseContract;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="billing_period_id") private BillingPeriod billingPeriod;
    @Column(name="issue_date",nullable=false) private LocalDate issueDate;
    @Column(name="due_date",nullable=false) private LocalDate dueDate;
    @Column(name="total_amount",nullable=false,precision=18,scale=2) private BigDecimal totalAmount=BigDecimal.ZERO;
    @Column(name="paid_amount",nullable=false,precision=18,scale=2) private BigDecimal paidAmount=BigDecimal.ZERO;
    @Column(nullable=false,length=20) private String status="UNPAID";
    @OneToMany(mappedBy="receivable",cascade=CascadeType.ALL,orphanRemoval=true) private List<ReceivableItem> items=new ArrayList<>();
    protected Receivable(){}
    public Receivable(String ref,Customer customer,Unit unit,BillingPeriod period,LocalDate issue,LocalDate due){referenceCode=ref;this.customer=customer;this.unit=unit;billingPeriod=period;issueDate=issue;dueDate=due;}
    public Receivable(String ref,CommercialTenant tenant,CommercialSpace space,LeaseContract contract,BillingPeriod period,LocalDate issue,LocalDate due){referenceCode=ref;commercialTenant=tenant;commercialSpace=space;leaseContract=contract;billingPeriod=period;issueDate=issue;dueDate=due;}
    public void addItem(ServiceType type,String description,BigDecimal quantity,BigDecimal unitPrice,LocalDate start,LocalDate end){ReceivableItem item=new ReceivableItem(this,type,description,quantity,unitPrice,start,end);items.add(item);totalAmount=totalAmount.add(item.getAmount());}
    public void clearItems(){items.clear();totalAmount=BigDecimal.ZERO;status="UNPAID";}
    public void recordPayment(BigDecimal amount){paidAmount=paidAmount.add(amount);status=paidAmount.compareTo(totalAmount)>=0?"PAID":"PARTIAL";}
    public BigDecimal outstanding(){return totalAmount.subtract(paidAmount);}
    public Long getId(){return id;}public String getReferenceCode(){return referenceCode;}public Customer getCustomer(){return customer;}public Unit getUnit(){return unit;}public CommercialTenant getCommercialTenant(){return commercialTenant;}public CommercialSpace getCommercialSpace(){return commercialSpace;}public LeaseContract getLeaseContract(){return leaseContract;}public boolean isCommercial(){return leaseContract!=null;}public BillingPeriod getBillingPeriod(){return billingPeriod;}public LocalDate getIssueDate(){return issueDate;}public LocalDate getDueDate(){return dueDate;}public BigDecimal getTotalAmount(){return totalAmount;}public BigDecimal getPaidAmount(){return paidAmount;}public String getStatus(){return status;}public List<ReceivableItem> getItems(){return items;}
}

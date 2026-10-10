package vn.com.apartment.billing;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.*;
@Entity @Table(name="payment_qr_requests") public class PaymentQrRequest{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="receivable_item_id") private ReceivableItem legacyReceivableItem;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receivable_id") private Receivable receivable;
 @OneToMany(mappedBy="qrRequest",cascade=CascadeType.ALL,orphanRemoval=true) private List<PaymentQrAllocation> allocations=new ArrayList<>();
 @Column(name="reference_code",nullable=false,unique=true,length=50) private String referenceCode;@Column(nullable=false,precision=18,scale=2) private BigDecimal amount;@Column(name="bank_code",nullable=false,length=20) private String bankCode;@Column(name="bank_account",nullable=false,length=50) private String bankAccount;@Column(nullable=false,length=20) private String status="PENDING";@Column(name="expires_at") private LocalDateTime expiresAt;@Column(name="paid_at") private LocalDateTime paidAt;@Column(name="created_at",nullable=false) private LocalDateTime createdAt=LocalDateTime.now();protected PaymentQrRequest(){}
 public PaymentQrRequest(Receivable receivable,String reference,String bankCode,String bankAccount,LocalDateTime expiresAt){this.receivable=receivable;referenceCode=reference;this.bankCode=bankCode;this.bankAccount=bankAccount;this.expiresAt=expiresAt;amount=BigDecimal.ZERO;}
 public void addAllocation(ReceivableItem item,BigDecimal value){allocations.add(new PaymentQrAllocation(this,item,value));amount=amount.add(value);}
 public void markPaid(LocalDateTime time){status="PAID";paidAt=time;}public void expire(){status="EXPIRED";}
 public Long getId(){return id;}public ReceivableItem getReceivableItem(){return allocations.isEmpty()?legacyReceivableItem:allocations.getFirst().getReceivableItem();}public Receivable getReceivable(){return receivable;}public List<PaymentQrAllocation> getAllocations(){return allocations;}public String getReferenceCode(){return referenceCode;}public BigDecimal getAmount(){return amount;}public String getBankCode(){return bankCode;}public String getBankAccount(){return bankAccount;}public String getStatus(){return status;}public LocalDateTime getExpiresAt(){return expiresAt;}public LocalDateTime getPaidAt(){return paidAt;}public LocalDateTime getCreatedAt(){return createdAt;}
}

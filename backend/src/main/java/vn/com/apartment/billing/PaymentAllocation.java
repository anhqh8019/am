package vn.com.apartment.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="payment_allocations")
public class PaymentAllocation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="payment_id")
    private Payment payment;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="receivable_item_id")
    private ReceivableItem receivableItem;

    @Column(nullable=false,precision=18,scale=2)
    private BigDecimal amount;

    protected PaymentAllocation() {}

    PaymentAllocation(Payment payment,ReceivableItem receivableItem,BigDecimal amount) {
        this.payment=payment;
        this.receivableItem=receivableItem;
        this.amount=amount;
    }

    public ReceivableItem getReceivableItem() { return receivableItem; }
    public BigDecimal getAmount() { return amount; }
}

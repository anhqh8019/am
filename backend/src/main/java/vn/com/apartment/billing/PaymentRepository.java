package vn.com.apartment.billing;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface PaymentRepository extends JpaRepository<Payment,Long>{
 @Query("""
   select distinct p from Payment p join fetch p.receivable r join fetch r.billingPeriod bp
   left join fetch r.customer left join fetch r.unit u left join fetch u.building
   left join fetch r.commercialTenant left join fetch r.commercialSpace s left join fetch s.building
   left join fetch p.allocations a left join fetch a.receivableItem
   where (:periodId is null or bp.id=:periodId) order by p.paymentDate desc, p.id desc
   """)
 List<Payment> searchDetailed(@Param("periodId") Long periodId);
}


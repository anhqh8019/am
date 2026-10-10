package vn.com.apartment.billing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ReceivableRepository extends JpaRepository<Receivable,Long>{
 boolean existsByUnitIdAndBillingPeriodId(Long unitId,Long periodId);
 Optional<Receivable> findByUnitIdAndBillingPeriodId(Long unitId,Long periodId);
 @Query("""
     select distinct r from Receivable r
     join fetch r.customer
     join fetch r.unit u
     join fetch u.building
     join fetch r.billingPeriod
     left join fetch r.items
     where (:periodId is null or r.billingPeriod.id = :periodId)
     order by r.dueDate desc, u.code
     """)
 List<Receivable> search(@Param("periodId") Long periodId);
}

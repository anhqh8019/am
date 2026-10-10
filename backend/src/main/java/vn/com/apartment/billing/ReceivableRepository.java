package vn.com.apartment.billing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ReceivableRepository extends JpaRepository<Receivable,Long>{
 boolean existsByUnitIdAndBillingPeriodId(Long unitId,Long periodId);
 Optional<Receivable> findByUnitIdAndBillingPeriodId(Long unitId,Long periodId);
 Optional<Receivable> findByLeaseContractIdAndBillingPeriodId(Long contractId,Long periodId);
 @Query("""
     select distinct r from Receivable r
     left join fetch r.customer
     left join fetch r.unit u
     left join fetch u.building
     left join fetch r.commercialTenant
     left join fetch r.commercialSpace s
     left join fetch s.building
     left join fetch r.leaseContract
     join fetch r.billingPeriod
     left join fetch r.items
     where (:periodId is null or r.billingPeriod.id = :periodId)
     order by r.dueDate desc, r.id desc
     """)
 List<Receivable> search(@Param("periodId") Long periodId);
}

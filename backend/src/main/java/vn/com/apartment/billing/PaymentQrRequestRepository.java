package vn.com.apartment.billing;
import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface PaymentQrRequestRepository extends JpaRepository<PaymentQrRequest,Long>{
 boolean existsByReceivableId(Long receivableId);
 @Query("select distinct q from PaymentQrRequest q left join fetch q.allocations a left join fetch a.receivableItem where q.receivable.id=:receivableId")
 List<PaymentQrRequest> findDetailedByReceivableId(@Param("receivableId") Long receivableId);
 @Query("select distinct q from PaymentQrRequest q join fetch q.receivable r left join fetch q.allocations a left join fetch a.receivableItem where q.referenceCode=:reference") Optional<PaymentQrRequest> findDetailedByReference(@Param("reference") String reference);
 @Query("select distinct q from PaymentQrRequest q join q.allocations a where a.receivableItem.id=:itemId and q.status='PENDING' order by q.createdAt desc") List<PaymentQrRequest> findPendingByItem(@Param("itemId") Long itemId);
 @Query("select distinct q from PaymentQrRequest q join fetch q.receivable r left join fetch r.unit left join fetch r.customer left join fetch r.commercialSpace left join fetch r.commercialTenant left join fetch q.allocations a left join fetch a.receivableItem order by q.createdAt desc") List<PaymentQrRequest> findAllDetailed();
}

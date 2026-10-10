package vn.com.apartment.billing;
import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface PaymentQrRequestRepository extends JpaRepository<PaymentQrRequest,Long>{
 @Query("select distinct q from PaymentQrRequest q join fetch q.receivable r left join fetch q.allocations a left join fetch a.receivableItem where q.referenceCode=:reference") Optional<PaymentQrRequest> findDetailedByReference(@Param("reference") String reference);
 @Query("select distinct q from PaymentQrRequest q join q.allocations a where a.receivableItem.id=:itemId and q.status='PENDING' order by q.createdAt desc") List<PaymentQrRequest> findPendingByItem(@Param("itemId") Long itemId);
 @Query("select distinct q from PaymentQrRequest q join fetch q.receivable r join fetch r.unit u join fetch r.customer left join fetch q.allocations a left join fetch a.receivableItem order by q.createdAt desc") List<PaymentQrRequest> findAllDetailed();
}

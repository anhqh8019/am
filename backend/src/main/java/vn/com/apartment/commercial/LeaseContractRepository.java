package vn.com.apartment.commercial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface LeaseContractRepository extends JpaRepository<LeaseContract,Long>{
    boolean existsByContractNumberIgnoreCase(String number);
    @Query("select c from LeaseContract c join fetch c.commercialSpace s join fetch s.building join fetch c.tenant order by c.startDate desc")
    List<LeaseContract> findAllDetailed();
    Optional<LeaseContract> findFirstByCommercialSpace_IdAndStatusOrderByStartDateDesc(Long spaceId,LeaseStatus status);
    boolean existsByCommercialSpace_IdAndStatus(Long spaceId,LeaseStatus status);
    boolean existsByTenant_IdAndStatus(Long tenantId,LeaseStatus status);
}

package vn.com.apartment.commercial;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface LeaseContractRepository extends JpaRepository<LeaseContract,Long>{
    boolean existsByContractNumberIgnoreCase(String number);
    @Query("select c from LeaseContract c join fetch c.commercialSpace s join fetch s.building join fetch c.tenant order by c.startDate desc")
    List<LeaseContract> findAllDetailed();
    Optional<LeaseContract> findFirstByCommercialSpace_IdAndStatusOrderByStartDateDesc(Long spaceId,LeaseStatus status);
    boolean existsByCommercialSpace_IdAndStatus(Long spaceId,LeaseStatus status);
    boolean existsByTenant_IdAndStatus(Long tenantId,LeaseStatus status);
    @Query("""
        select c from LeaseContract c
        join fetch c.commercialSpace s
        join fetch s.building
        join fetch c.tenant
        where c.status = vn.com.apartment.commercial.LeaseStatus.ACTIVE
          and c.startDate <= :periodEnd and c.endDate >= :periodStart
          and (:buildingId is null or s.building.id = :buildingId)
        order by s.building.code, s.code
        """)
    List<LeaseContract> findBillable(@Param("periodStart") LocalDate periodStart,@Param("periodEnd") LocalDate periodEnd,@Param("buildingId") Long buildingId);
}

package vn.com.apartment.resident;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitOccupancyRepository extends JpaRepository<UnitOccupancy,Long>{
    @Query("select o from UnitOccupancy o join fetch o.unit u join fetch u.building join fetch o.customer where o.customer.id in :ids order by u.code,o.startDate desc")
    List<UnitOccupancy> findDetailedByCustomerIds(@Param("ids") Collection<Long> ids);
    @Query("select o from UnitOccupancy o where o.unit.id=:unitId and o.primary=true and o.endDate is null")
    List<UnitOccupancy> findActivePrimaryByUnitId(@Param("unitId") Long unitId);
    @Query("select o from UnitOccupancy o join fetch o.customer where o.unit.id=:unitId and o.occupancyRole=vn.com.apartment.resident.OccupancyRole.OWNER and o.endDate is null")
    List<UnitOccupancy> findActiveOwnersByUnitId(@Param("unitId") Long unitId);
    @Query("select o from UnitOccupancy o join fetch o.customer where o.unit.id=:unitId and o.endDate is null order by o.primary desc,o.startDate")
    List<UnitOccupancy> findActiveByUnitId(@Param("unitId") Long unitId);
    boolean existsByUnitIdAndCustomerIdAndEndDateIsNull(Long unitId,Long customerId);
}

package vn.com.apartment.commercial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface ElectricityReadingRepository extends JpaRepository<ElectricityReading,Long>{
    boolean existsByMeterIdAndPeriodCode(Long meterId,String periodCode);
    @Query("select r from ElectricityReading r join fetch r.meter m join fetch m.commercialSpace order by r.readingDate desc,r.id desc")
    List<ElectricityReading> findAllDetailed();
    @Query("select r from ElectricityReading r join fetch r.meter m where m.commercialSpace.id = :spaceId and r.periodCode = :periodCode order by m.meterCode")
    List<ElectricityReading> findForBilling(@Param("spaceId") Long spaceId,@Param("periodCode") String periodCode);
}

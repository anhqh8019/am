package vn.com.apartment.commercial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface ElectricityMeterRepository extends JpaRepository<ElectricityMeter,Long>{
    boolean existsByMeterCodeIgnoreCase(String code);
    @Query("select m from ElectricityMeter m join fetch m.commercialSpace s join fetch s.building order by m.meterCode")
    List<ElectricityMeter> findAllDetailed();
}

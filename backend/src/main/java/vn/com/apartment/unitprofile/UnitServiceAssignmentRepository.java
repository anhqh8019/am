package vn.com.apartment.unitprofile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UnitServiceAssignmentRepository extends JpaRepository<UnitServiceAssignment,Long>{
    List<UnitServiceAssignment> findByUnitIdAndActiveTrueOrderByMandatoryDescServiceNameAsc(Long unitId);
    boolean existsByUnitIdAndServiceCodeAndActiveTrue(Long unitId,String serviceCode);
    boolean existsByLicensePlateAndActiveTrue(String licensePlate);
}

package vn.com.apartment.commercial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CommercialTenantRepository extends JpaRepository<CommercialTenant,Long>{
    boolean existsByCodeIgnoreCase(String code);
    List<CommercialTenant> findAllByActiveTrueOrderByNameAsc();
}

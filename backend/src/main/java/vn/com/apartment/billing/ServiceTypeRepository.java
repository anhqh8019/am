package vn.com.apartment.billing;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ServiceTypeRepository extends JpaRepository<ServiceType,Long>{Optional<ServiceType> findByCode(String code);}


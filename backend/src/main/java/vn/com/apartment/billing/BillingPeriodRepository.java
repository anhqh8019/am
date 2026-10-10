package vn.com.apartment.billing;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BillingPeriodRepository extends JpaRepository<BillingPeriod,Long>{Optional<BillingPeriod> findByCode(String code);List<BillingPeriod> findAllByOrderByPeriodStartDesc();}


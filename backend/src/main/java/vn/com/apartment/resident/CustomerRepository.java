package vn.com.apartment.resident;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer,Long>{
    boolean existsByCustomerCodeIgnoreCase(String code);
    boolean existsByCustomerCodeIgnoreCaseAndIdNot(String code,Long id);
    boolean existsByNationalId(String nationalId);
    boolean existsByNationalIdAndIdNot(String nationalId,Long id);
    java.util.Optional<Customer> findByNationalId(String nationalId);
    @Query("""
        select distinct c from Customer c
        left join UnitOccupancy o on o.customer=c
        left join o.unit u left join u.building b
        where (:buildingId is null or b.id=:buildingId)
          and (:keyword is null or lower(c.customerCode) like lower(concat('%',:keyword,'%'))
            or lower(c.fullName) like lower(concat('%',:keyword,'%'))
            or lower(c.phone) like lower(concat('%',:keyword,'%')))
        order by c.fullName
        """)
    List<Customer> search(@Param("buildingId") Long buildingId,@Param("keyword") String keyword);
}

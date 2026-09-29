package vn.com.apartment.commercial;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommercialSpaceRepository extends JpaRepository<CommercialSpace,Long> {
    boolean existsByBuildingIdAndCodeIgnoreCase(Long buildingId,String code);
    boolean existsByBuildingIdAndCodeIgnoreCaseAndIdNot(Long buildingId,String code,Long id);
    @Query("""
        select s from CommercialSpace s join fetch s.building b
        where (:buildingId is null or b.id=:buildingId)
          and (:keyword is null or lower(s.code) like lower(concat('%',:keyword,'%')) or lower(s.location) like lower(concat('%',:keyword,'%')))
        order by b.code,s.code
        """)
    List<CommercialSpace> search(@Param("buildingId") Long buildingId,@Param("keyword") String keyword);
}

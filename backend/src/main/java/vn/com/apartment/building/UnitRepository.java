package vn.com.apartment.building;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    boolean existsByBuildingIdAndCodeIgnoreCase(Long buildingId, String code);
    boolean existsByBuildingIdAndCodeIgnoreCaseAndIdNot(Long buildingId, String code, Long id);

    @Query("""
        select u from Unit u join fetch u.building b
        where (:buildingId is null or b.id = :buildingId)
          and (:keyword is null or lower(u.code) like lower(concat('%', :keyword, '%')))
        order by b.code, u.code
        """)
    List<Unit> search(@Param("buildingId") Long buildingId, @Param("keyword") String keyword);
}

package vn.com.apartment.tariff;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceTariffRepository extends JpaRepository<ServiceTariff, Long> {
    @Query("""
        select t from ServiceTariff t
        left join fetch t.building b
        order by t.serviceCode, t.effectiveFrom desc, t.id desc
        """)
    List<ServiceTariff> findAllWithBuilding();

    @Query("""
        select t from ServiceTariff t
        where t.serviceCode = :serviceCode
          and ((:buildingId is null and t.building is null) or t.building.id = :buildingId)
          and t.active = true
        order by t.effectiveFrom desc, t.id desc
        """)
    List<ServiceTariff> findActiveForScope(@Param("serviceCode") String serviceCode,
                                           @Param("buildingId") Long buildingId);

    @Query("""
        select t from ServiceTariff t
        where t.serviceCode = :serviceCode
          and ((:buildingId is null and t.building is null) or t.building.id = :buildingId)
        order by t.effectiveFrom desc, t.id desc
        """)
    List<ServiceTariff> findForScope(@Param("serviceCode") String serviceCode,
                                     @Param("buildingId") Long buildingId);

    @Query("""
        select t from ServiceTariff t
        where t.serviceCode = :serviceCode
          and (t.building is null or t.building.id = :buildingId)
          and t.effectiveFrom <= :date
          and (t.effectiveTo is null or t.effectiveTo >= :date)
        order by case when t.building is not null then 0 else 1 end,
                 t.effectiveFrom desc, t.id desc
        """)
    List<ServiceTariff> findEffective(@Param("serviceCode") String serviceCode,
                                      @Param("buildingId") Long buildingId,
                                      @Param("date") LocalDate date);


    @Query("""
    select t from ServiceTariff t
    left join fetch t.building b
    where t.serviceCode = :serviceCode
      and (t.building is null or t.building.id = :buildingId)
    order by case when t.building is not null then 0 else 1 end,
             t.effectiveFrom desc, t.id desc
    """)
    List<ServiceTariff> findApplicable(
            @Param("serviceCode") String serviceCode,
            @Param("buildingId") Long buildingId
    );
}


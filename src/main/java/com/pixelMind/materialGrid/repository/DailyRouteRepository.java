package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.DailyRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DailyRouteRepository extends JpaRepository<DailyRoute, Long> {

    @Query("""
            select d from DailyRoute d
            where d.deleted = false
              and (:date is null or d.date = :date)
              and (:vehicleId is null or d.vehicle.id = :vehicleId)
              and (:routeId is null or d.route.id = :routeId)
            """)
    Page<DailyRoute> search(
            @Param("date") LocalDate date,
            @Param("vehicleId") Long vehicleId,
            @Param("routeId") Long routeId,
            Pageable pageable);

    boolean existsByVehicleIdAndDeletedFalse(Long vehicleId);

    boolean existsByRouteIdAndDeletedFalse(Long routeId);

    //boolean existsByPriceRateIdAndDeletedFalse(Long priceRateId);

    @Query("""
            select d from DailyRoute d
            where d.deleted = false
              and d.date in :dates
              and d.vehicle.id in :vehicleIds
              and d.route.id in :routeIds
            """)
    List<DailyRoute> findPotentialDuplicates(
            @Param("dates") Collection<LocalDate> dates,
            @Param("vehicleIds") Collection<Long> vehicleIds,
            @Param("routeIds") Collection<Long> routeIds);

    List<DailyRoute> findByVehicleIdAndDateAndDeletedFalse(
            Long vehicleId,
            LocalDate date);

    @Query("""
            select d from DailyRoute d
            join fetch d.route
            where d.deleted = false
              and d.vehicle.id = :vehicleId
              and d.date between :startDate and :endDate
            order by d.date asc
            """)
    List<DailyRoute> findByVehicleIdAndDateBetween(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
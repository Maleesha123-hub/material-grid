package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
              and (:priceRateId is null or d.priceRate.id = :priceRateId)
            """)
    Page<DailyRoute> search(
            @Param("date") LocalDate date,
            @Param("vehicleId") Long vehicleId,
            @Param("routeId") Long routeId,
            @Param("priceRateId") Long priceRateId,
            Pageable pageable);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByRouteId(Long routeId);

    boolean existsByPriceRateId(Long priceRateId);

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

    /**
     * Used by the Daily Route PDF report. Returns a List (not Optional)
     * deliberately, so the service layer can distinguish "no record" from
     * "unexpectedly more than one record" and report each as a distinct,
     * clear error rather than letting Spring Data throw a generic
     * IncorrectResultSizeDataAccessException.
     */
    List<DailyRoute> findByVehicleIdAndDateAndDeletedFalse(Long vehicleId, LocalDate date);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from DailyRoute e
            where e.vehicle.id = :vehicleId
            and e.date = :date
            and e.deleted = false
            """)
    BigDecimal sumAmountsByVehicleIdAndDate(Long vehicleId, LocalDate date);

    @Query("""
            select COUNT(e.id)
            from DailyRoute e
            where e.vehicle.id = :vehicleId
            and e.date = :date
            and e.deleted = false
            """)
    Integer loadCountByVehicleIdAndDate(Long vehicleId, LocalDate date);

    List<DailyRoute> findByVehicleIdAndDateBetweenAndDeletedFalse(
            Long vehicleId,
            LocalDate startDate,
            LocalDate endDate
    );
}
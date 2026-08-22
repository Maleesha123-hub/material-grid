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

    List<DailyRoute> findByVehicleIdAndDateAndDeletedFalse(Long vehicleId, LocalDate date);

    /**
     * MODIFIED: now also `join fetch d.route` alongside the existing
     * `d.priceRate` fetch. The payment receipt now reads BOTH
     * priceRate.getPrice() and route.getRouteCode()/route.getKm() for every
     * record while consolidating by date - fetching both here in one query
     * is what keeps that N+1-free (JPA allows multiple join fetches in a
     * single JPQL query; this still produces exactly one SQL statement).
     */
    @Query("""
            select d from DailyRoute d
            join fetch d.priceRate
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
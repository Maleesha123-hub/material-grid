package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.DailyRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DailyRouteRepository extends JpaRepository<DailyRoute, Long> {

    @Query("""
            select d from DailyRoute d
            left join d.fileHistory fh
            where d.deleted = false
              and (:date is null or d.date = :date)
              and (:createdDateFrom is null or d.createdDate >= :createdDateFrom)
              and (:createdDateTo is null or d.createdDate < :createdDateTo)
              and (:billNumber is null or lower(d.billNumber) like lower(concat('%', :billNumber, '%')))
              and (:vehicleId is null or d.vehicle.id = :vehicleId)
              and (:routeId is null or d.route.id = :routeId)
              and (:fileHistoryId is null or fh.id = :fileHistoryId)
            """)
    Page<DailyRoute> search(
            @Param("date") LocalDate date,
            @Param("createdDateFrom") LocalDateTime createdDateFrom,
            @Param("createdDateTo") LocalDateTime createdDateTo,
            @Param("billNumber") String billNumber,
            @Param("vehicleId") Long vehicleId,
            @Param("routeId") Long routeId,
            @Param("fileHistoryId") Long fileHistoryId,
            Pageable pageable);

    boolean existsByVehicleIdAndDeletedFalse(Long vehicleId);

    boolean existsByRouteIdAndDeletedFalse(Long routeId);

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

    @Query("""
            select d
            from DailyRoute d
            where d.deleted = false
              and d.vehicle.id = :vehicleId
            order by d.date asc
            """)
    List<DailyRoute> findByVehicle(
            @Param("vehicleId") Long vehicleId
    );

}
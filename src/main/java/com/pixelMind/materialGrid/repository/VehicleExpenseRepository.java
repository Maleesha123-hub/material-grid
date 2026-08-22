package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.VehicleExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface VehicleExpenseRepository extends JpaRepository<VehicleExpense, Long> {

    Page<VehicleExpense> findByDeletedFalse(Pageable pageable);

    Page<VehicleExpense> findByVehicleIdAndDeletedFalse(Long vehicleId, Pageable pageable);

    Page<VehicleExpense> findByDateBetweenAndDeletedFalse(LocalDate from, LocalDate to, Pageable pageable);

    boolean existsByVehicleId(Long vehicleId);

    @Query("""
            select coalesce(sum(e.expenses), 0) from VehicleExpense e
            where e.vehicle.id = :vehicleId and e.date = :date and e.deleted = false
            """)
    BigDecimal sumExpensesByVehicleIdAndDate(@Param("vehicleId") Long vehicleId, @Param("date") LocalDate date);

    @Query("""
            select coalesce(sum(e.expenses), 0) from VehicleExpense e
            where e.vehicle.id = :vehicleId
              and e.date between :startDate and :endDate
              and e.deleted = false
            """)
    BigDecimal sumExpensesByVehicleIdAndDateBetween(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * NEW: backs the payment receipt's per-date Paid Amount consolidation
     * (spec sections 11-13). Fetches every raw expense in the range in ONE
     * query; DailyRouteReportServiceImpl groups these by date and sums both
     * the per-date and range-wide totals in memory - no query per date, no
     * query per row (see spec section 30).
     */
    List<VehicleExpense> findByVehicleIdAndDateBetweenAndDeletedFalse(
            Long vehicleId, LocalDate startDate, LocalDate endDate);
}
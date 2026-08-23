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

    Page<VehicleExpense> findByVehicleIdAndDeletedFalse(
            Long vehicleId,
            Pageable pageable);

    Page<VehicleExpense> findByDateBetweenAndDeletedFalse(
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    boolean existsByVehicleIdAndDeletedFalse(Long vehicleId);

    @Query("""
            select coalesce(sum(e.expenses), 0)
            from VehicleExpense e
            where e.deleted = false
              and e.vehicle.id = :vehicleId
              and e.date = :date
            """)
    BigDecimal sumExpensesByVehicleIdAndDate(
            @Param("vehicleId") Long vehicleId,
            @Param("date") LocalDate date);

    @Query("""
            select coalesce(sum(e.expenses), 0)
            from VehicleExpense e
            where e.deleted = false
              and e.vehicle.id = :vehicleId
              and e.date between :startDate and :endDate
            """)
    BigDecimal sumExpensesByVehicleIdAndDateBetween(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<VehicleExpense> findByVehicleIdAndDateBetweenAndDeletedFalse(
            Long vehicleId,
            LocalDate startDate,
            LocalDate endDate);
}
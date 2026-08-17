package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.VehicleExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VehicleExpenseRepository extends JpaRepository<VehicleExpense, Long> {
//
//    Page<VehicleExpense> findByDeletedFalse(Pageable pageable);
//
//    Page<VehicleExpense> findByVehicleIdAndDeletedFalse(Long vehicleId, Pageable pageable);
//
//    Page<VehicleExpense> findByDateBetweenAndDeletedFalse(LocalDate from, LocalDate to, Pageable pageable);
//
//    boolean existsByVehicleId(Long vehicleId);
//
//    /**
//     * SUM aggregate used by the Daily Route PDF report's "Paid Amount".
//     * COALESCE guards against a null SUM when there are zero matching rows,
//     * so the caller never has to null-check.
//     */
//    @Query("""
//            select coalesce(sum(e.expenses), 0) from VehicleExpense e
//            where e.vehicle.id = :vehicleId and e.date = :date and e.deleted = false
//            """)
//    BigDecimal sumExpensesByVehicleIdAndDate(@Param("vehicleId") Long vehicleId, @Param("date") LocalDate date);
}
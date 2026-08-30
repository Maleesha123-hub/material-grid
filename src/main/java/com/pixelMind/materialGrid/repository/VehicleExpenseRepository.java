package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.VehicleExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface VehicleExpenseRepository extends JpaRepository<VehicleExpense, Long> {

    @Query("""
            select e from VehicleExpense e
            left join e.fileHistory fh
            where e.deleted = false
              and (:date is null or e.date = :date)
              and (:createdDateFrom is null or e.createdDate >= :createdDateFrom)
              and (:createdDateTo is null or e.createdDate < :createdDateTo)
              and (:vehicleId is null or e.vehicle.id = :vehicleId)
              and (:fileHistoryId is null or fh.id = :fileHistoryId)
            """)
    Page<VehicleExpense> search(
            @Param("date") LocalDate date,
            @Param("createdDateFrom") LocalDateTime createdDateFrom,
            @Param("createdDateTo") LocalDateTime createdDateTo,
            @Param("vehicleId") Long vehicleId,
            @Param("fileHistoryId") Long fileHistoryId,
            Pageable pageable);

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
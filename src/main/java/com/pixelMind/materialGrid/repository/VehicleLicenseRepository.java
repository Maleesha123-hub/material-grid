package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.VehicleLicense;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VehicleLicenseRepository extends JpaRepository<VehicleLicense, Long> {

    @Query("""
            select vl from VehicleLicense vl
            left join vl.fileHistory fh
            where vl.deleted = false
              and (:licenseId is null or vl.license.id = :licenseId)
              and (:vehicleId is null or vl.vehicle.id = :vehicleId)
              and (:date is null or vl.date = :date)
              and (:createdDateFrom is null or vl.createdDate >= :createdDateFrom)
              and (:createdDateTo is null or vl.createdDate < :createdDateTo)
              and (:status is null or vl.status = :status)
              and (:fileHistoryId is null or fh.id = :fileHistoryId)
            """)
    Page<VehicleLicense> search(
            @Param("licenseId") Long licenseId,
            @Param("vehicleId") Long vehicleId,
            @Param("date") LocalDate date,
            @Param("createdDateFrom") LocalDateTime createdDateFrom,
            @Param("createdDateTo") LocalDateTime createdDateTo,
            @Param("status") VehicleLicenseStatus status,
            @Param("fileHistoryId") Long fileHistoryId,
            Pageable pageable);

    Page<VehicleLicense> findByVehicleIdAndDeletedFalse(
            Long vehicleId,
            Pageable pageable);

    Page<VehicleLicense> findByLicenseIdAndDeletedFalse(
            Long licenseId,
            Pageable pageable);

    Page<VehicleLicense> findByStatusAndDeletedFalse(
            VehicleLicenseStatus status,
            Pageable pageable);

    Page<VehicleLicense> findByVehicleIdAndStatusAndDeletedFalse(
            Long vehicleId,
            VehicleLicenseStatus status,
            Pageable pageable);

    boolean existsByVehicleIdAndDeletedFalse(Long vehicleId);

    boolean existsByLicenseIdAndDeletedFalse(Long licenseId);

    boolean existsByVehicleIdAndLicenseIdAndDeletedFalse(
            Long vehicleId,
            Long licenseId);

    boolean existsByVehicleIdAndLicenseIdAndDateAndDeletedFalse(Long vehicleId, Long licenseId, LocalDate date);

    List<VehicleLicense> findByVehicleIdInAndLicenseIdInAndDeletedFalse(
            Collection<Long> vehicleIds,
            Collection<Long> licenseIds);

    List<VehicleLicense> findByVehicleIdAndDateAndDeletedFalse(
            Long vehicleId,
            LocalDate date);

    List<VehicleLicense> findByVehicleIdAndDateBetweenAndDeletedFalse(
            Long vehicleId,
            LocalDate startDate,
            LocalDate endDate);
}
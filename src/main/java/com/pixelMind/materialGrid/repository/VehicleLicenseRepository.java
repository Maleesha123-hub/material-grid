package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.VehicleLicense;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface VehicleLicenseRepository extends JpaRepository<VehicleLicense, Long> {

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
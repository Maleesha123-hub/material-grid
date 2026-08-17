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

    Page<VehicleLicense> findByVehicleId(Long vehicleId, Pageable pageable);

    Page<VehicleLicense> findByLicenseId(Long licenseId, Pageable pageable);

    Page<VehicleLicense> findByStatus(VehicleLicenseStatus status, Pageable pageable);

    Page<VehicleLicense> findByVehicleIdAndStatus(Long vehicleId, VehicleLicenseStatus status, Pageable pageable);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByLicenseId(Long licenseId);

    List<VehicleLicense> findByVehicleIdInAndLicenseIdIn(Collection<Long> vehicleIds, Collection<Long> licenseIds);

    /**
     * Used by the Daily Route PDF report. Returns a List, not Optional -
     * same rationale as DailyRouteRepository#findByVehicleIdAndDateAndDeletedFalse:
     * lets the service tell "no license" apart from "duplicate license data",
     * rather than an opaque Spring Data exception.
     *
     * Deliberately does NOT filter by license date range - see the mandatory
     * rule in the report service.
     */
    List<VehicleLicense> findByVehicleIdAndDate(Long vehicleId, LocalDate date);
}
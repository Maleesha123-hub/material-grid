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

    boolean existsByVehicleIdAndLicenseId(Long vehicleId, Long licenseId);

    List<VehicleLicense> findByVehicleIdInAndLicenseIdIn(Collection<Long> vehicleIds, Collection<Long> licenseIds);

    List<VehicleLicense> findByVehicleIdAndDate(Long vehicleId, LocalDate date);

    /**
     * NEW: backs the date-range Daily Route report's Licence Fee. See
     * DailyRouteReportServiceImpl#resolveLicenceFeeForRange for the
     * distinct-license, no-double-counting rule this feeds, and its one
     * known limitation. Deliberately does NOT filter by License date
     * range - same mandatory rule as the single-date version.
     */
    List<VehicleLicense> findByVehicleIdAndDateBetween(Long vehicleId, LocalDate startDate, LocalDate endDate);
}
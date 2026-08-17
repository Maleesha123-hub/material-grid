package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LicenseRepository extends JpaRepository<License, Long> {

    boolean existsByLicenseCode(String licenseCode);

    /**
     * Fetches every License whose [startDate, endDate] range overlaps
     * [minDate, maxDate] - one query per Excel import covering the whole
     * file's date span, refined per-row in memory by the caller, instead of
     * one query per distinct date.
     *
     * Condition: startDate <= maxDate AND endDate >= minDate.
     */
    List<License> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate maxDate, LocalDate minDate);
}
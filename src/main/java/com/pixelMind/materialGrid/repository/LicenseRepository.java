package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.License;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT l FROM License l
            WHERE (:startDate IS NULL OR l.endDate >= :startDate)
              AND (:endDate IS NULL OR l.startDate <= :endDate)
            """)
    Page<License> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}


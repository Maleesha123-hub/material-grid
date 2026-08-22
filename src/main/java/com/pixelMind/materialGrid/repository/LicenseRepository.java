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

    boolean existsByLicenseCodeAndDeletedFalse(String licenseCode);

    @Query("""
            SELECT l FROM License l
            WHERE l.deleted = false
              AND l.startDate <= :maxDate
              AND l.endDate >= :minDate
            """)
    List<License> findByDateRange(
            @Param("maxDate") LocalDate maxDate,
            @Param("minDate") LocalDate minDate);

    @Query("""
            SELECT l FROM License l
            WHERE l.deleted = false
              AND (:startDate IS NULL OR l.endDate >= :startDate)
              AND (:endDate IS NULL OR l.startDate <= :endDate)
            """)
    Page<License> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END
            FROM License l
            WHERE l.deleted = false
              AND (:id IS NULL OR l.id <> :id)
              AND l.startDate <= :endDate
              AND l.endDate >= :startDate
            """)
    boolean existsOverlapping(
            @Param("id") Long id,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
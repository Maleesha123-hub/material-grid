package com.pixelMind.materialGrid.entity;

import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Deliberately a full entity, not a @ManyToMany join: the relationship
 * itself carries business data (date, status) that a bare join table can't
 * express. No unique constraint on (vehicle_id, license_id) - see
 * VehicleLicenseServiceImpl class Javadoc for why duplicates across time are
 * a valid, expected business scenario (license renewal), not a data error.
 */
@Entity
@Table(name = "vehicle_licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class VehicleLicense extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VehicleLicenseStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "file_history_id", nullable = true)
    private FileHistory fileHistory;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

}

package com.pixelMind.materialGrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Shared audit columns for the modules introduced in this iteration (Route,
 * Vehicle, License, VehicleLicense, VehicleExpense, DailyRoute).
 *
 * Note: the pre-existing entities (User, UserSession, PriceRate) are
 * deliberately NOT retrofitted to extend this class. They already have their
 * own working, tested @PrePersist/@PreUpdate audit logic; migrating them to
 * a shared superclass now would touch stable code for a cosmetic-only gain
 * and risk regressions in a part of the system this task didn't ask to
 * change. New modules use this base class going forward; a follow-up
 * refactor to unify all entities under it is a reasonable future cleanup,
 * not a requirement of this change.
 *
 * Population strategy matches the existing project convention (see
 * config/JpaConfig.java): addedBy/modifiedBy are set explicitly in the
 * service layer via SecurityUtil.getCurrentUsername(), not through Spring
 * Data JPA's @CreatedBy/@LastModifiedBy auditing infrastructure.
 *
 * @SuperBuilder + a protected no-args constructor here is required so that
 * subclasses annotated with @SuperBuilder can chain builder calls that set
 * these inherited fields (e.g. Route.builder().routeCode(x).createdBy(y)...) -
 * Lombok's @SuperBuilder only includes superclass fields in the generated
 * builder when the superclass is itself @SuperBuilder-annotated.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseAuditableEntity {

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onBaseCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.modifiedDate = now;
    }

    @PreUpdate
    protected void onBaseUpdate() {
        this.modifiedDate = LocalDateTime.now();
    }
}

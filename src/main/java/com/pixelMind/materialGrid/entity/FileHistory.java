package com.pixelMind.materialGrid.entity;

import com.pixelMind.materialGrid.entity.enums.FileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A write-once audit log entry for a completed Excel upload - never updated
 * after creation (no PUT endpoint exists or is planned), so this does NOT
 * extend BaseAuditableEntity: that class models mutable audited business
 * entities (createdBy/modifiedBy/version for something that changes over
 * time), whereas this is a single immutable fact ("user X uploaded file Y
 * at time Z") with its own, spec-mandated field names (uploadedBy /
 * uploadedDate) that don't map onto created/modified semantics. No
 * @Version either, for the same reason - nothing ever updates this row.
 *
 * Deliberately has NO @OneToMany back-reference to Vehicle/VehicleExpense/
 * DailyRoute - nothing in the current requirements needs "list everything
 * uploaded in this file", and adding one would risk N+1 queries or an
 * unbounded object graph for no current benefit (see class Javadoc on
 * Vehicle/VehicleExpense/DailyRoute's fileHistory field for the other
 * direction of this relationship).
 */
@Entity
@Table(
        name = "file_history",
        uniqueConstraints = @UniqueConstraint(name = "uk_file_history_name_type", columnNames = {"file_name", "file_type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    private FileType fileType;

    @Column(name = "uploaded_by", nullable = false, length = 50)
    private String uploadedBy;

    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
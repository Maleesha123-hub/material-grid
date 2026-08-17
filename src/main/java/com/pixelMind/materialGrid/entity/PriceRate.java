package com.pixelMind.materialGrid.entity;

import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PriceRateStatus status;

    @Column(name = "added_by", nullable = false, length = 50)
    private String addedBy;

    @Column(name = "added_date", nullable = false, updatable = false)
    private LocalDateTime addedDate;

    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.addedDate = now;
        this.modifiedDate = now;
        if (this.status == null) {
            this.status = PriceRateStatus.INACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
    }
}

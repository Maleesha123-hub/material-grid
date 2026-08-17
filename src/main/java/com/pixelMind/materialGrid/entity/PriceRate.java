package com.pixelMind.materialGrid.entity;

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
    @Column(name = "idprice_rate")
    private Long id;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    private boolean active;

    private String createdBy;

    private LocalDateTime createdDate;

    private String updatedBy;

    private LocalDateTime updatedDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}

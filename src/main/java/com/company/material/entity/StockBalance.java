package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"materialId", "warehouseId"})
})
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long materialId;

    @Column(length = 30)
    private String materialCode;

    @Column(length = 100)
    private String materialName;

    @Column(length = 30)
    private String category;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(length = 20)
    private String warehouseCode;

    @Column(precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 4)
    private BigDecimal avgCost;

    @Column(precision = 16, scale = 2)
    private BigDecimal totalAmount;

    private LocalDateTime lastInTime;

    private LocalDateTime lastOutTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.quantity == null) {
            this.quantity = BigDecimal.ZERO;
        }
        if (this.avgCost == null) {
            this.avgCost = BigDecimal.ZERO;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

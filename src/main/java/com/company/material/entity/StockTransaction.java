package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_transactions")
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String transactionNo;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false)
    private Long materialId;

    @Column(length = 30)
    private String materialCode;

    @Column(length = 100)
    private String materialName;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(length = 20)
    private String warehouseCode;

    @Column(precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 16, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 50)
    private String department;

    private Long supplierId;

    @Column(length = 50)
    private String supplierName;

    @Column(length = 200)
    private String remark;

    @Column(length = 30)
    private String operator;

    private LocalDateTime transactionTime;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.transactionTime == null) {
            this.transactionTime = LocalDateTime.now();
        }
        if (this.quantity != null && this.unitPrice != null && this.totalAmount == null) {
            this.totalAmount = this.quantity.multiply(this.unitPrice);
        }
    }
}

package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "material_budgets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"fiscalYear", "budgetType", "targetCode"})
})
public class MaterialBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4)
    private String fiscalYear;

    @Column(nullable = false, length = 20)
    private String budgetType;

    @Column(length = 50)
    private String targetCode;

    @Column(length = 100)
    private String targetName;

    @Column(precision = 16, scale = 2)
    private BigDecimal budgetAmount;

    @Column(length = 200)
    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

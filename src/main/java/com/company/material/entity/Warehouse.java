package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String warehouseCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String manager;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "启用";
    }
}

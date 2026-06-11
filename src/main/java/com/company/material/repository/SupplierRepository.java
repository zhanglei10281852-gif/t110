package com.company.material.repository;

import com.company.material.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    boolean existsBySupplierCode(String supplierCode);
    Page<Supplier> findByStatus(String status, Pageable pageable);
    Page<Supplier> findByCategory(String category, Pageable pageable);
}

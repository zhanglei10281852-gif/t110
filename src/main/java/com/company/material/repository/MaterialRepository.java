package com.company.material.repository;

import com.company.material.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByMaterialCode(String materialCode);
    boolean existsByMaterialCode(String materialCode);
    Page<Material> findByCategory(String category, Pageable pageable);
    Page<Material> findByStatus(String status, Pageable pageable);

    @Query("SELECT m FROM Material m WHERE m.name LIKE %:kw% OR m.materialCode LIKE %:kw%")
    Page<Material> search(@Param("kw") String kw, Pageable pageable);

    long countByCategory(String category);
}

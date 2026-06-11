package com.company.material.repository;

import com.company.material.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByDepartment(String department, Pageable pageable);
    long countByStatus(String status);
}

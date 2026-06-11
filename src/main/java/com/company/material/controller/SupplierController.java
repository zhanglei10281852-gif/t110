package com.company.material.controller;

import com.company.material.entity.Supplier;
import com.company.material.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Supplier supplier) {
        if (supplier.getSupplierCode() == null || supplier.getName() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "供应商编码和名称为必填"));
        }
        if (supplierRepository.existsBySupplierCode(supplier.getSupplierCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "供应商编码已存在"));
        }
        supplier.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierRepository.save(supplier));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Supplier> result;
        if (status != null && !status.isBlank()) {
            result = supplierRepository.findByStatus(status, pr);
        } else if (category != null && !category.isBlank()) {
            result = supplierRepository.findByCategory(category, pr);
        } else {
            result = supplierRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return supplierRepository.findById(id)
                .map(s -> ResponseEntity.ok((Object) s))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Supplier body) {
        return supplierRepository.findById(id).map(s -> {
            if (body.getName() != null) s.setName(body.getName());
            if (body.getContactPerson() != null) s.setContactPerson(body.getContactPerson());
            if (body.getPhone() != null) s.setPhone(body.getPhone());
            if (body.getAddress() != null) s.setAddress(body.getAddress());
            if (body.getCategory() != null) s.setCategory(body.getCategory());
            if (body.getStatus() != null) s.setStatus(body.getStatus());
            return ResponseEntity.ok((Object) supplierRepository.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }
}

package com.company.material.controller;

import com.company.material.entity.Warehouse;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Warehouse warehouse) {
        if (warehouse.getWarehouseCode() == null || warehouse.getName() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "仓库编码和名称为必填"));
        }
        if (warehouseRepository.existsByWarehouseCode(warehouse.getWarehouseCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "仓库编码已存在"));
        }
        warehouse.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseRepository.save(warehouse));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(warehouseRepository.findByStatus(status));
        }
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return warehouseRepository.findById(id)
                .map(w -> ResponseEntity.ok((Object) w))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Warehouse body) {
        return warehouseRepository.findById(id).map(w -> {
            if (body.getName() != null) w.setName(body.getName());
            if (body.getLocation() != null) w.setLocation(body.getLocation());
            if (body.getManager() != null) w.setManager(body.getManager());
            if (body.getPhone() != null) w.setPhone(body.getPhone());
            if (body.getStatus() != null) w.setStatus(body.getStatus());
            return ResponseEntity.ok((Object) warehouseRepository.save(w));
        }).orElse(ResponseEntity.notFound().build());
    }
}

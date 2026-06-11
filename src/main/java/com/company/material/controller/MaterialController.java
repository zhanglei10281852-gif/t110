package com.company.material.controller;

import com.company.material.entity.Material;
import com.company.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialRepository materialRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Material material) {
        if (material.getMaterialCode() == null || material.getName() == null
                || material.getCategory() == null || material.getUnit() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "物料编码、名称、类别、单位为必填"));
        }
        if (materialRepository.existsByMaterialCode(material.getMaterialCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "物料编码已存在"));
        }
        material.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(materialRepository.save(material));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Material> result;
        if (keyword != null && !keyword.isBlank()) {
            result = materialRepository.search(keyword, pr);
        } else if (category != null && !category.isBlank()) {
            result = materialRepository.findByCategory(category, pr);
        } else if (status != null && !status.isBlank()) {
            result = materialRepository.findByStatus(status, pr);
        } else {
            result = materialRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return materialRepository.findById(id)
                .map(m -> ResponseEntity.ok((Object) m))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Material body) {
        return materialRepository.findById(id).map(m -> {
            if (body.getName() != null) m.setName(body.getName());
            if (body.getCategory() != null) m.setCategory(body.getCategory());
            if (body.getUnit() != null) m.setUnit(body.getUnit());
            if (body.getSpecification() != null) m.setSpecification(body.getSpecification());
            if (body.getReferencePrice() != null) m.setReferencePrice(body.getReferencePrice());
            if (body.getSafetyStock() != null) m.setSafetyStock(body.getSafetyStock());
            if (body.getRemark() != null) m.setRemark(body.getRemark());
            if (body.getStatus() != null) m.setStatus(body.getStatus());
            return ResponseEntity.ok((Object) materialRepository.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!materialRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        materialRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/stats/category")
    public ResponseEntity<?> statsByCategory() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", materialRepository.count());
        return ResponseEntity.ok(stats);
    }
}

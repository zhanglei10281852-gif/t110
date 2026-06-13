package com.company.material.controller;

import com.company.material.entity.StockBalance;
import com.company.material.entity.StockTransaction;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.StockBalanceRepository;
import com.company.material.repository.StockTransactionRepository;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockTransactionRepository transactionRepository;
    private final StockBalanceRepository balanceRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;

    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody StockTransaction tx) {
        if (tx.getType() == null || tx.getMaterialId() == null || tx.getWarehouseId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "类型、物料ID、仓库ID为必填"));
        }
        if (!"采购入库".equals(tx.getType()) && !"领用出库".equals(tx.getType())
                && !"调拨入库".equals(tx.getType()) && !"调拨出库".equals(tx.getType())
                && !"盘盈入库".equals(tx.getType()) && !"盘亏出库".equals(tx.getType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "类型无效，有效值：采购入库/领用出库/调拨入库/调拨出库/盘盈入库/盘亏出库"));
        }
        tx.setId(null);
        if (tx.getTransactionNo() == null || tx.getTransactionNo().isBlank()) {
            String prefix = "采购入库".equals(tx.getType()) ? "RK"
                    : "领用出库".equals(tx.getType()) ? "CK" : "QT";
            tx.setTransactionNo(prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        }
        materialRepository.findById(tx.getMaterialId()).ifPresent(m -> {
            if (tx.getMaterialCode() == null) tx.setMaterialCode(m.getMaterialCode());
            if (tx.getMaterialName() == null) tx.setMaterialName(m.getName());
            if (tx.getUnitPrice() == null && m.getReferencePrice() != null) {
                tx.setUnitPrice(m.getReferencePrice());
            }
        });
        warehouseRepository.findById(tx.getWarehouseId()).ifPresent(w -> {
            if (tx.getWarehouseCode() == null) tx.setWarehouseCode(w.getWarehouseCode());
        });
        StockTransaction saved = transactionRepository.save(tx);
        updateBalance(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private void updateBalance(StockTransaction tx) {
        StockBalance balance = balanceRepository
                .findByMaterialIdAndWarehouseId(tx.getMaterialId(), tx.getWarehouseId())
                .orElseGet(() -> {
                    StockBalance nb = new StockBalance();
                    nb.setMaterialId(tx.getMaterialId());
                    nb.setMaterialCode(tx.getMaterialCode());
                    nb.setMaterialName(tx.getMaterialName());
                    nb.setWarehouseId(tx.getWarehouseId());
                    nb.setWarehouseCode(tx.getWarehouseCode());
                    materialRepository.findById(tx.getMaterialId()).ifPresent(m -> nb.setCategory(m.getCategory()));
                    nb.setQuantity(BigDecimal.ZERO);
                    nb.setAvgCost(BigDecimal.ZERO);
                    nb.setTotalAmount(BigDecimal.ZERO);
                    return nb;
                });
        boolean isIn = tx.getType().contains("入库");
        BigDecimal txQty = tx.getQuantity() != null ? tx.getQuantity() : BigDecimal.ZERO;
        BigDecimal txAmt = tx.getTotalAmount() != null ? tx.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal currentQty = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        BigDecimal currentAmt = balance.getTotalAmount() != null ? balance.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newQty, newAmt, newAvg;
        if (isIn) {
            newQty = currentQty.add(txQty);
            newAmt = currentAmt.add(txAmt);
            balance.setLastInTime(tx.getTransactionTime() != null ? tx.getTransactionTime() : LocalDateTime.now());
        } else {
            newQty = currentQty.subtract(txQty);
            if (newQty.compareTo(BigDecimal.ZERO) < 0) newQty = BigDecimal.ZERO;
            BigDecimal outAmt = currentQty.compareTo(BigDecimal.ZERO) > 0
                    ? txQty.multiply(balance.getAvgCost() != null ? balance.getAvgCost() : BigDecimal.ZERO)
                    : txAmt;
            newAmt = currentAmt.subtract(outAmt);
            if (newAmt.compareTo(BigDecimal.ZERO) < 0) newAmt = BigDecimal.ZERO;
            balance.setLastOutTime(tx.getTransactionTime() != null ? tx.getTransactionTime() : LocalDateTime.now());
        }
        newAvg = newQty.compareTo(BigDecimal.ZERO) > 0
                ? newAmt.divide(newQty, 4, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        balance.setQuantity(newQty);
        balance.setTotalAmount(newAmt);
        balance.setAvgCost(newAvg);
        materialRepository.findById(tx.getMaterialId()).ifPresent(m -> balance.setCategory(m.getCategory()));
        balanceRepository.save(balance);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long materialId) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("transactionTime").descending());
        Page<StockTransaction> result;
        if (materialId != null) {
            result = transactionRepository.findAll(pr);
            List<StockTransaction> filtered = new ArrayList<>();
            for (StockTransaction t : result.getContent()) {
                if (t.getMaterialId().equals(materialId)) filtered.add(t);
            }
            return ResponseEntity.ok(filtered);
        }
        result = transactionRepository.findAll(pr);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/balances")
    public ResponseEntity<?> listBalances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("totalAmount").descending());
        Page<StockBalance> result;
        if (category != null && !category.isBlank()) {
            List<StockBalance> all = balanceRepository.findAll();
            List<StockBalance> filtered = new ArrayList<>();
            for (StockBalance b : all) {
                if (category.equals(b.getCategory())) filtered.add(b);
            }
            return ResponseEntity.ok(filtered);
        }
        result = balanceRepository.findAll(pr);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/balances/{id}")
    public ResponseEntity<?> updateBalance(@PathVariable Long id, @RequestBody StockBalance body) {
        return balanceRepository.findById(id).map(b -> {
            if (body.getQuantity() != null) b.setQuantity(body.getQuantity());
            if (body.getAvgCost() != null) b.setAvgCost(body.getAvgCost());
            if (body.getTotalAmount() != null) b.setTotalAmount(body.getTotalAmount());
            if (body.getLastInTime() != null) b.setLastInTime(body.getLastInTime());
            if (body.getLastOutTime() != null) b.setLastOutTime(body.getLastOutTime());
            return ResponseEntity.ok((Object) balanceRepository.save(b));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/balances/init")
    public ResponseEntity<?> initBalances(@RequestBody List<StockBalance> balances) {
        int count = 0;
        for (StockBalance b : balances) {
            StockBalance existing = balanceRepository
                    .findByMaterialIdAndWarehouseId(b.getMaterialId(), b.getWarehouseId())
                    .orElse(null);
            if (existing != null) {
                if (b.getQuantity() != null) existing.setQuantity(b.getQuantity());
                if (b.getAvgCost() != null) existing.setAvgCost(b.getAvgCost());
                if (b.getTotalAmount() != null) existing.setTotalAmount(b.getTotalAmount());
                if (b.getLastInTime() != null) existing.setLastInTime(b.getLastInTime());
                if (b.getLastOutTime() != null) existing.setLastOutTime(b.getLastOutTime());
                materialRepository.findById(b.getMaterialId()).ifPresent(m -> {
                    existing.setCategory(m.getCategory());
                    if (existing.getMaterialCode() == null) existing.setMaterialCode(m.getMaterialCode());
                    if (existing.getMaterialName() == null) existing.setMaterialName(m.getName());
                });
                balanceRepository.save(existing);
            } else {
                b.setId(null);
                materialRepository.findById(b.getMaterialId()).ifPresent(m -> {
                    b.setCategory(m.getCategory());
                    if (b.getMaterialCode() == null) b.setMaterialCode(m.getMaterialCode());
                    if (b.getMaterialName() == null) b.setMaterialName(m.getName());
                });
                warehouseRepository.findById(b.getWarehouseId()).ifPresent(w -> {
                    if (b.getWarehouseCode() == null) b.setWarehouseCode(w.getWarehouseCode());
                });
                if (b.getQuantity() == null) b.setQuantity(BigDecimal.ZERO);
                if (b.getAvgCost() == null) b.setAvgCost(BigDecimal.ZERO);
                if (b.getTotalAmount() == null) {
                    b.setTotalAmount(b.getQuantity().multiply(b.getAvgCost()));
                }
                balanceRepository.save(b);
            }
            count++;
        }
        return ResponseEntity.ok(Map.of("message", "初始化库存余额成功", "count", count));
    }

    @PostMapping("/transactions/batch")
    public ResponseEntity<?> batchCreateTransactions(@RequestBody List<StockTransaction> txs) {
        int count = 0;
        for (StockTransaction tx : txs) {
            if (tx.getType() != null && tx.getMaterialId() != null && tx.getWarehouseId() != null) {
                tx.setId(null);
                if (tx.getTransactionNo() == null || tx.getTransactionNo().isBlank()) {
                    String prefix = tx.getType().contains("入库") ? "RK" : "CK";
                    tx.setTransactionNo(prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                            + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
                }
                materialRepository.findById(tx.getMaterialId()).ifPresent(m -> {
                    if (tx.getMaterialCode() == null) tx.setMaterialCode(m.getMaterialCode());
                    if (tx.getMaterialName() == null) tx.setMaterialName(m.getName());
                });
                warehouseRepository.findById(tx.getWarehouseId()).ifPresent(w -> {
                    if (tx.getWarehouseCode() == null) tx.setWarehouseCode(w.getWarehouseCode());
                });
                StockTransaction saved = transactionRepository.save(tx);
                updateBalance(saved);
                count++;
            }
        }
        return ResponseEntity.ok(Map.of("message", "批量录入成功", "count", count));
    }
}

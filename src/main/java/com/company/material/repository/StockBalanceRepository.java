package com.company.material.repository;

import com.company.material.entity.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByMaterialIdAndWarehouseId(Long materialId, Long warehouseId);

    List<StockBalance> findByMaterialId(Long materialId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM StockBalance s")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM StockBalance s WHERE s.lastOutTime < :threshold OR s.lastOutTime IS NULL")
    BigDecimal sumStagnantAmount(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT s FROM StockBalance s WHERE s.quantity > 0")
    List<StockBalance> findAllPositiveInventory();

    @Query("SELECT s FROM StockBalance s WHERE s.lastOutTime < :threshold OR s.lastOutTime IS NULL ORDER BY s.totalAmount DESC")
    List<StockBalance> findStagnantMaterials(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT s.category, COUNT(s), COALESCE(SUM(s.totalAmount), 0) FROM StockBalance s GROUP BY s.category")
    List<Object[]> sumByCategory();

    @Query("SELECT s FROM StockBalance s WHERE s.quantity > 0 AND s.lastOutTime < :threshold ORDER BY s.totalAmount DESC")
    List<StockBalance> findSlowMovingMaterials(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT s FROM StockBalance s WHERE s.quantity > 0 AND s.lastOutTime >= :threshold ORDER BY s.totalAmount DESC")
    List<StockBalance> findFastMovingMaterials(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT s.category, COALESCE(AVG(s.totalAmount), 0) FROM StockBalance s GROUP BY s.category")
    List<Object[]> avgAmountByCategory();

    @Query("SELECT s.materialId, s.materialCode, s.materialName, s.category, s.quantity, s.avgCost, s.totalAmount, s.lastOutTime " +
           "FROM StockBalance s WHERE s.quantity > 0")
    List<Object[]> findAllWithInventory();

    @Query("SELECT s.warehouseCode, COALESCE(SUM(s.totalAmount), 0), COUNT(s) FROM StockBalance s GROUP BY s.warehouseCode")
    List<Object[]> sumByWarehouse();
}

package com.company.material.repository;

import com.company.material.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM StockTransaction t WHERE t.type = :type AND t.transactionTime BETWEEN :start AND :end")
    BigDecimal sumAmountByTypeAndTime(@Param("type") String type,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM StockTransaction t WHERE t.type = '采购入库' AND t.transactionTime BETWEEN :start AND :end")
    BigDecimal sumPurchaseAmount(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT t.materialId, t.materialCode, t.materialName, COALESCE(SUM(t.totalAmount), 0) as amount " +
           "FROM StockTransaction t WHERE t.type = '领用出库' AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY t.materialId, t.materialCode, t.materialName " +
           "ORDER BY amount DESC")
    List<Object[]> sumOutboundAmountByMaterial(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("SELECT t.materialId, MAX(t.transactionTime) FROM StockTransaction t " +
           "WHERE t.type = '领用出库' GROUP BY t.materialId")
    List<Object[]> findLastOutboundTimeByMaterial();

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionTime, '%Y-%m') as month, " +
           "t.type, COALESCE(SUM(t.totalAmount), 0) " +
           "FROM StockTransaction t WHERE t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY month, t.type ORDER BY month")
    List<Object[]> sumAmountByMonthAndType(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("SELECT t.department, COALESCE(SUM(t.totalAmount), 0) FROM StockTransaction t " +
           "WHERE t.type = '领用出库' AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY t.department ORDER BY 2 DESC")
    List<Object[]> sumOutboundAmountByDepartment(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionTime, '%Y-%m') as month, " +
           "t.department, COALESCE(SUM(t.totalAmount), 0) " +
           "FROM StockTransaction t WHERE t.type = '领用出库' AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY month, t.department ORDER BY month")
    List<Object[]> sumOutboundAmountByMonthAndDept(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query("SELECT t.materialId, t.materialCode, t.materialName, " +
           "FUNCTION('DATE_FORMAT', t.transactionTime, '%Y-%m') as month, " +
           "COALESCE(AVG(t.unitPrice), 0) " +
           "FROM StockTransaction t WHERE t.type = '采购入库' AND t.transactionTime BETWEEN :start AND :end " +
           "AND t.materialId IN :materialIds " +
           "GROUP BY t.materialId, t.materialCode, t.materialName, month ORDER BY month")
    List<Object[]> avgPurchasePriceByMaterialAndMonth(@Param("materialIds") List<Long> materialIds,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Query("SELECT t.materialId, COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
           "WHERE t.type = '领用出库' AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY t.materialId")
    List<Object[]> sumOutboundQuantityByMaterial(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionTime, '%Y-%m') as month, " +
           "COALESCE(SUM(t.totalAmount), 0) FROM StockTransaction t " +
           "WHERE t.type = :type AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY month ORDER BY month")
    List<Object[]> sumAmountByMonth(@Param("type") String type,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
           "WHERE t.materialId = :materialId AND t.type = '领用出库' AND t.transactionTime BETWEEN :start AND :end")
    BigDecimal sumOutboundQuantityByMaterialAndTime(@Param("materialId") Long materialId,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    List<StockTransaction> findByMaterialIdOrderByTransactionTimeDesc(Long materialId);
}

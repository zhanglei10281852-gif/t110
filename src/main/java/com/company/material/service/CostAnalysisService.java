package com.company.material.service;

import com.company.material.entity.Material;
import com.company.material.entity.MaterialBudget;
import com.company.material.entity.StockBalance;
import com.company.material.entity.StockTransaction;
import com.company.material.repository.MaterialBudgetRepository;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.StockBalanceRepository;
import com.company.material.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostAnalysisService {

    private final StockTransactionRepository transactionRepository;
    private final StockBalanceRepository balanceRepository;
    private final MaterialRepository materialRepository;
    private final MaterialBudgetRepository budgetRepository;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public Map<String, Object> getDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        YearMonth lastMonth = currentMonth.minusMonths(1);
        LocalDateTime lastMonthStart = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal totalInventoryValue = safeDec(balanceRepository.sumTotalAmount());
        BigDecimal monthInbound = safeDec(transactionRepository.sumAmountByTypeAndTime("采购入库", monthStart, monthEnd));
        BigDecimal monthOutbound = safeDec(transactionRepository.sumAmountByTypeAndTime("领用出库", monthStart, monthEnd));
        BigDecimal monthPurchase = safeDec(transactionRepository.sumPurchaseAmount(monthStart, monthEnd));

        BigDecimal lastMonthOutbound = safeDec(transactionRepository.sumAmountByTypeAndTime("领用出库", lastMonthStart, lastMonthEnd));
        BigDecimal avgInventory = totalInventoryValue.add(totalInventoryValue).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal turnoverRate = avgInventory.compareTo(ZERO) > 0
                ? monthOutbound.add(lastMonthOutbound).divide(avgInventory, 4, RoundingMode.HALF_UP)
                : ZERO;

        LocalDateTime stagnantThreshold = now.minusDays(90);
        BigDecimal stagnantAmount = safeDec(balanceRepository.sumStagnantAmount(stagnantThreshold));

        long safetyWarningCount = countSafetyStockWarning();

        result.put("totalInventoryValue", totalInventoryValue);
        result.put("monthInboundAmount", monthInbound);
        result.put("monthOutboundAmount", monthOutbound);
        result.put("monthPurchaseAmount", monthPurchase);
        result.put("inventoryTurnoverRate", turnoverRate);
        result.put("stagnantMaterialAmount", stagnantAmount);
        result.put("safetyStockWarningCount", safetyWarningCount);
        return result;
    }

    private long countSafetyStockWarning() {
        List<StockBalance> balances = balanceRepository.findAllPositiveInventory();
        long count = 0;
        for (StockBalance b : balances) {
            if (b.getMaterialId() != null) {
                materialRepository.findById(b.getMaterialId()).ifPresent(m -> {
                    // safetyStock handled via side effect not possible, use different approach
                });
            }
        }
        Map<Long, Integer> safetyMap = new HashMap<>();
        for (Material m : materialRepository.findAll()) {
            if (m.getSafetyStock() != null) {
                safetyMap.put(m.getId(), m.getSafetyStock());
            }
        }
        for (StockBalance b : balances) {
            Integer safety = safetyMap.get(b.getMaterialId());
            if (safety != null && b.getQuantity() != null && b.getQuantity().intValue() < safety) {
                count++;
            }
        }
        return count;
    }

    public Map<String, Object> getAbcAnalysis() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yearAgo = now.minusYears(1);

        List<Object[]> raw = transactionRepository.sumOutboundAmountByMaterial(yearAgo, now);
        List<MaterialAbcItem> items = new ArrayList<>();
        BigDecimal totalAmount = ZERO;

        for (Object[] row : raw) {
            Long materialId = ((Number) row[0]).longValue();
            String materialCode = (String) row[1];
            String materialName = (String) row[2];
            BigDecimal amount = safeDec(row[3]);
            String category = "";
            Optional<Material> mOpt = materialRepository.findById(materialId);
            if (mOpt.isPresent()) {
                category = mOpt.get().getCategory();
            }
            items.add(new MaterialAbcItem(materialId, materialCode, materialName, category, amount));
            totalAmount = totalAmount.add(amount);
        }

        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        List<MaterialAbcItem> classA = new ArrayList<>();
        List<MaterialAbcItem> classB = new ArrayList<>();
        List<MaterialAbcItem> classC = new ArrayList<>();

        BigDecimal cumulative = ZERO;
        for (MaterialAbcItem item : items) {
            BigDecimal ratio = totalAmount.compareTo(ZERO) > 0
                    ? item.getAmount().divide(totalAmount, 6, RoundingMode.HALF_UP)
                    : ZERO;
            BigDecimal cumulativeBefore = cumulative;
            cumulative = cumulative.add(item.getAmount());
            BigDecimal cumulativeRatio = totalAmount.compareTo(ZERO) > 0
                    ? cumulative.divide(totalAmount, 6, RoundingMode.HALF_UP)
                    : ZERO;
            item.setRatio(ratio);
            item.setCumulativeRatio(cumulativeRatio);

            if (cumulativeBefore.compareTo(new BigDecimal("0.70")) < 0) {
                item.setCategoryClass("A");
                classA.add(item);
            } else if (cumulativeBefore.compareTo(new BigDecimal("0.90")) < 0) {
                item.setCategoryClass("B");
                classB.add(item);
            } else {
                item.setCategoryClass("C");
                classC.add(item);
            }
        }

        int totalCount = items.size();
        BigDecimal classAAmount = sumAmount(classA);
        BigDecimal classBAmount = sumAmount(classB);
        BigDecimal classCAmount = sumAmount(classC);

        Map<String, Object> classAInfo = buildClassInfo("A", "重点管理", classA, totalCount, classAAmount, totalAmount);
        Map<String, Object> classBInfo = buildClassInfo("B", "常规管理", classB, totalCount, classBAmount, totalAmount);
        Map<String, Object> classCInfo = buildClassInfo("C", "简化管理", classC, totalCount, classCAmount, totalAmount);

        result.put("totalMaterialCount", totalCount);
        result.put("totalAnnualOutboundAmount", totalAmount);
        result.put("classA", classAInfo);
        result.put("classB", classBInfo);
        result.put("classC", classCInfo);
        result.put("allItems", items);
        return result;
    }

    private Map<String, Object> buildClassInfo(String cls, String desc, List<MaterialAbcItem> items,
                                                int totalCount, BigDecimal classAmount, BigDecimal totalAmount) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("class", cls);
        info.put("description", desc);
        info.put("materialCount", items.size());
        info.put("countRatio", totalCount > 0
                ? new BigDecimal(items.size()).divide(new BigDecimal(totalCount), 4, RoundingMode.HALF_UP)
                : ZERO);
        info.put("amount", classAmount);
        info.put("amountRatio", totalAmount.compareTo(ZERO) > 0
                ? classAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                : ZERO);
        info.put("materials", items);
        return info;
    }

    private BigDecimal sumAmount(List<MaterialAbcItem> items) {
        BigDecimal sum = ZERO;
        for (MaterialAbcItem item : items) {
            sum = sum.add(item.getAmount());
        }
        return sum;
    }

    public Map<String, Object> getStagnantAnalysis(Integer topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        Map<Integer, List<Map<String, Object>>> stagnantByDays = new LinkedHashMap<>();
        int[] thresholds = {30, 60, 90, 180};

        List<StockBalance> allBalances = balanceRepository.findAllPositiveInventory();
        Map<Long, LocalDateTime> lastOutboundMap = new HashMap<>();
        for (Object[] row : transactionRepository.findLastOutboundTimeByMaterial()) {
            Long materialId = ((Number) row[0]).longValue();
            LocalDateTime lastTime = (LocalDateTime) row[1];
            lastOutboundMap.put(materialId, lastTime);
        }

        for (int days : thresholds) {
            LocalDateTime threshold = now.minusDays(days);
            List<Map<String, Object>> list = new ArrayList<>();
            BigDecimal totalAmount = ZERO;
            for (StockBalance b : allBalances) {
                LocalDateTime lastOut = lastOutboundMap.get(b.getMaterialId());
                boolean isStagnant = lastOut == null || lastOut.isBefore(threshold);
                if (isStagnant && b.getQuantity() != null && b.getQuantity().compareTo(ZERO) > 0) {
                    Map<String, Object> item = buildStagnantItem(b, lastOut, now);
                    list.add(item);
                    totalAmount = totalAmount.add(safeDec(b.getTotalAmount()));
                }
            }
            list.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));
            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("days", days);
            bucket.put("materialCount", list.size());
            bucket.put("totalAmount", totalAmount);
            bucket.put("materials", list);
            stagnantByDays.put(days, Collections.singletonList(bucket));
        }

        List<Map<String, Object>> trend = buildStagnantTrend();

        int n = topN != null ? topN : 20;
        List<StockBalance> topStagnant = balanceRepository.findAllPositiveInventory();
        List<Map<String, Object>> recommendation = new ArrayList<>();
        for (StockBalance b : topStagnant) {
            LocalDateTime lastOut = lastOutboundMap.get(b.getMaterialId());
            if ((lastOut == null || lastOut.isBefore(now.minusDays(90)))
                    && b.getQuantity() != null && b.getQuantity().compareTo(ZERO) > 0) {
                recommendation.add(buildStagnantItem(b, lastOut, now));
            }
        }
        recommendation.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));
        if (recommendation.size() > n) {
            recommendation = recommendation.subList(0, n);
        }

        result.put("stagnantByDays", stagnantByDays);
        result.put("trend", trend);
        result.put("recommendationTopN", recommendation);
        return result;
    }

    private Map<String, Object> buildStagnantItem(StockBalance b, LocalDateTime lastOut, LocalDateTime now) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("materialId", b.getMaterialId());
        item.put("materialCode", b.getMaterialCode());
        item.put("materialName", b.getMaterialName());
        item.put("category", b.getCategory());
        item.put("warehouseCode", b.getWarehouseCode());
        item.put("quantity", b.getQuantity());
        item.put("avgCost", b.getAvgCost());
        item.put("totalAmount", safeDec(b.getTotalAmount()));
        item.put("lastOutboundTime", lastOut);
        long stagnantDays = lastOut != null
                ? java.time.Duration.between(lastOut, now).toDays()
                : b.getCreatedAt() != null
                    ? java.time.Duration.between(b.getCreatedAt(), now).toDays()
                    : 0;
        item.put("stagnantDays", stagnantDays);
        return item;
    }

    private List<Map<String, Object>> buildStagnantTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.toLocalDate()).minusMonths(i);
            LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);
            LocalDateTime threshold90 = monthEnd.minusDays(90);
            BigDecimal stagnant90 = safeDec(balanceRepository.sumStagnantAmount(threshold90));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.toString());
            point.put("stagnant90DaysAmount", stagnant90);
            trend.add(point);
        }
        return trend;
    }

    public Map<String, Object> getTurnoverAnalysis() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start30 = now.minusDays(30);

        List<StockBalance> balances = balanceRepository.findAllPositiveInventory();
        Map<Long, BigDecimal> outboundQtyMap = new HashMap<>();
        for (Object[] row : transactionRepository.sumOutboundQuantityByMaterial(start30, now)) {
            Long materialId = ((Number) row[0]).longValue();
            BigDecimal qty = safeDec(row[1]);
            outboundQtyMap.put(materialId, qty);
        }

        List<Map<String, Object>> fastItems = new ArrayList<>();
        List<Map<String, Object>> slowItems = new ArrayList<>();
        Map<String, List<BigDecimal>> categoryTurnovers = new HashMap<>();

        for (StockBalance b : balances) {
            if (b.getQuantity() == null || b.getQuantity().compareTo(ZERO) <= 0) continue;
            BigDecimal outQty = outboundQtyMap.getOrDefault(b.getMaterialId(), ZERO);
            BigDecimal dailyAvg = outQty.divide(new BigDecimal("30"), 6, RoundingMode.HALF_UP);
            BigDecimal turnoverDays = dailyAvg.compareTo(ZERO) > 0
                    ? b.getQuantity().divide(dailyAvg, 2, RoundingMode.HALF_UP)
                    : new BigDecimal("9999");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", b.getMaterialId());
            item.put("materialCode", b.getMaterialCode());
            item.put("materialName", b.getMaterialName());
            item.put("category", b.getCategory());
            item.put("avgInventoryQty", b.getQuantity());
            item.put("avgInventoryAmount", safeDec(b.getTotalAmount()));
            item.put("monthOutboundQty", outQty);
            item.put("dailyOutboundAvg", dailyAvg);
            item.put("turnoverDays", turnoverDays);

            if (b.getCategory() != null) {
                categoryTurnovers.computeIfAbsent(b.getCategory(), k -> new ArrayList<>()).add(turnoverDays);
            }

            if (turnoverDays.compareTo(new BigDecimal("30")) <= 0) {
                fastItems.add(item);
            } else if (turnoverDays.compareTo(new BigDecimal("90")) > 0) {
                slowItems.add(item);
            }
        }

        fastItems.sort((a, b) -> ((BigDecimal) a.get("turnoverDays")).compareTo((BigDecimal) b.get("turnoverDays")));
        slowItems.sort((a, b) -> ((BigDecimal) b.get("turnoverDays")).compareTo((BigDecimal) a.get("turnoverDays")));

        List<Map<String, Object>> categoryStats = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> e : categoryTurnovers.entrySet()) {
            BigDecimal sum = ZERO;
            for (BigDecimal d : e.getValue()) sum = sum.add(d);
            BigDecimal avg = e.getValue().size() > 0
                    ? sum.divide(new BigDecimal(e.getValue().size()), 2, RoundingMode.HALF_UP)
                    : ZERO;
            Map<String, Object> cs = new LinkedHashMap<>();
            cs.put("category", e.getKey());
            cs.put("materialCount", e.getValue().size());
            cs.put("avgTurnoverDays", avg);
            categoryStats.add(cs);
        }
        categoryStats.sort((a, b) -> ((BigDecimal) a.get("avgTurnoverDays")).compareTo((BigDecimal) b.get("avgTurnoverDays")));

        result.put("fastTurnoverMaterials", fastItems);
        result.put("slowTurnoverMaterials", slowItems);
        result.put("categoryTurnoverComparison", categoryStats);
        return result;
    }

    public Map<String, Object> getCostTrendAnalysis() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start12 = now.minusMonths(12);

        List<Object[]> inbound = transactionRepository.sumAmountByMonth("采购入库", start12, now);
        List<Object[]> outbound = transactionRepository.sumAmountByMonth("领用出库", start12, now);

        List<Map<String, Object>> trend = new ArrayList<>();
        Map<String, BigDecimal> inMap = new HashMap<>();
        Map<String, BigDecimal> outMap = new HashMap<>();
        for (Object[] row : inbound) inMap.put((String) row[0], safeDec(row[1]));
        for (Object[] row : outbound) outMap.put((String) row[0], safeDec(row[1]));

        Map<String, BigDecimal> monthInventory = new HashMap<>();
        BigDecimal currentInventory = safeDec(balanceRepository.sumTotalAmount());
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.from(now.toLocalDate()).minusMonths(i);
            monthInventory.put(ym.toString(), currentInventory);
        }

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.toLocalDate()).minusMonths(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.toString());
            point.put("inboundCost", inMap.getOrDefault(ym.toString(), ZERO));
            point.put("outboundCost", outMap.getOrDefault(ym.toString(), ZERO));
            point.put("inventoryValue", monthInventory.getOrDefault(ym.toString(), ZERO));
            trend.add(point);
        }
        result.put("monthlyTrend", trend);

        List<Map<String, Object>> priceVolatility = getPriceVolatility();
        result.put("purchasePriceVolatility", priceVolatility);
        return result;
    }

    private List<Map<String, Object>> getPriceVolatility() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start12 = now.minusMonths(12);
        List<Object[]> topAmount = transactionRepository.sumOutboundAmountByMaterial(start12, now);
        List<Long> topMaterialIds = new ArrayList<>();
        int limit = Math.min(10, topAmount.size());
        for (int i = 0; i < limit; i++) {
            topMaterialIds.add(((Number) topAmount.get(i)[0]).longValue());
        }
        if (topMaterialIds.isEmpty()) return new ArrayList<>();

        List<Object[]> priceRows = transactionRepository.avgPurchasePriceByMaterialAndMonth(topMaterialIds, start12, now);
        Map<Long, List<Map<String, Object>>> materialPrices = new LinkedHashMap<>();
        Map<Long, String> materialNames = new HashMap<>();
        Map<Long, String> materialCodes = new HashMap<>();

        for (Object[] row : priceRows) {
            Long materialId = ((Number) row[0]).longValue();
            String code = (String) row[1];
            String name = (String) row[2];
            String month = (String) row[3];
            BigDecimal price = safeDec(row[4]);
            materialNames.put(materialId, name);
            materialCodes.put(materialId, code);
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("month", month);
            pm.put("avgPrice", price);
            materialPrices.computeIfAbsent(materialId, k -> new ArrayList<>()).add(pm);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> e : materialPrices.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", e.getKey());
            item.put("materialCode", materialCodes.get(e.getKey()));
            item.put("materialName", materialNames.get(e.getKey()));
            List<Map<String, Object>> prices = e.getValue();
            item.put("monthlyPrices", prices);
            if (prices.size() >= 2) {
                BigDecimal first = (BigDecimal) prices.get(0).get("avgPrice");
                BigDecimal last = (BigDecimal) prices.get(prices.size() - 1).get("avgPrice");
                BigDecimal changeRate = first.compareTo(ZERO) > 0
                        ? last.subtract(first).divide(first, 4, RoundingMode.HALF_UP)
                        : ZERO;
                item.put("priceChangeRate", changeRate);
                item.put("priceAbnormal", changeRate.compareTo(new BigDecimal("0.10")) > 0);
            } else {
                item.put("priceChangeRate", ZERO);
                item.put("priceAbnormal", false);
            }
            result.add(item);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("priceChangeRate")).compareTo((BigDecimal) a.get("priceChangeRate")));
        return result;
    }

    public Map<String, Object> getDepartmentExpenseAnalysis() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = YearMonth.from(now.toLocalDate()).atDay(1).atStartOfDay();
        LocalDateTime start12 = now.minusMonths(12);

        List<Object[]> monthByDept = transactionRepository.sumOutboundAmountByDepartment(monthStart, now);
        BigDecimal totalMonth = ZERO;
        for (Object[] row : monthByDept) totalMonth = totalMonth.add(safeDec(row[1]));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Object[] row : monthByDept) {
            String dept = (String) row[0];
            if (dept == null || dept.isBlank()) dept = "未分配";
            BigDecimal amount = safeDec(row[1]);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("department", dept);
            item.put("amount", amount);
            item.put("ratio", totalMonth.compareTo(ZERO) > 0
                    ? amount.divide(totalMonth, 4, RoundingMode.HALF_UP)
                    : ZERO);
            ranking.add(item);
        }
        result.put("monthlyRanking", ranking);

        List<Object[]> trendRows = transactionRepository.sumOutboundAmountByMonthAndDept(start12, now);
        Map<String, List<Map<String, Object>>> deptTrendMap = new LinkedHashMap<>();
        for (Object[] row : trendRows) {
            String month = (String) row[0];
            String dept = (String) row[1];
            if (dept == null || dept.isBlank()) dept = "未分配";
            BigDecimal amount = safeDec(row[2]);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month);
            point.put("amount", amount);
            deptTrendMap.computeIfAbsent(dept, k -> new ArrayList<>()).add(point);
        }
        List<Map<String, Object>> deptTrends = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : deptTrendMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("department", e.getKey());
            item.put("monthlyTrend", e.getValue());
            deptTrends.add(item);
        }
        result.put("departmentMonthlyTrends", deptTrends);
        return result;
    }

    public Map<String, Object> getBudgetExecution(String fiscalYear) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (fiscalYear == null || fiscalYear.isBlank()) {
            fiscalYear = String.valueOf(LocalDate.now().getYear());
        }

        List<MaterialBudget> purchaseBudgets = budgetRepository.findByFiscalYearAndBudgetType(fiscalYear, "年度采购总预算");
        List<MaterialBudget> deptBudgets = budgetRepository.findByFiscalYearAndBudgetType(fiscalYear, "部门用料预算");

        LocalDateTime now = LocalDateTime.now();
        YearMonth currentYm = YearMonth.from(now.toLocalDate());
        LocalDateTime yearStart = LocalDate.parse(fiscalYear + "-01-01").atStartOfDay();
        LocalDateTime yearEnd = LocalDate.parse(fiscalYear + "-12-31").atTime(23, 59, 59);

        BigDecimal actualPurchase = safeDec(transactionRepository.sumPurchaseAmount(yearStart, yearEnd));
        Map<String, Object> purchaseBudgetInfo = new LinkedHashMap<>();
        BigDecimal totalPurchaseBudget = ZERO;
        for (MaterialBudget b : purchaseBudgets) totalPurchaseBudget = totalPurchaseBudget.add(safeDec(b.getBudgetAmount()));
        purchaseBudgetInfo.put("budgetAmount", totalPurchaseBudget);
        purchaseBudgetInfo.put("actualAmount", actualPurchase);
        purchaseBudgetInfo.put("executionRate", totalPurchaseBudget.compareTo(ZERO) > 0
                ? actualPurchase.divide(totalPurchaseBudget, 4, RoundingMode.HALF_UP)
                : ZERO);
        purchaseBudgetInfo.put("remainingBudget", totalPurchaseBudget.subtract(actualPurchase));
        purchaseBudgetInfo.put("overBudget", actualPurchase.compareTo(totalPurchaseBudget) > 0);
        purchaseBudgetInfo.put("warning", actualPurchase.compareTo(totalPurchaseBudget) >= 0);
        result.put("annualPurchaseBudget", purchaseBudgetInfo);

        List<Object[]> actualByDept = transactionRepository.sumOutboundAmountByDepartment(yearStart, yearEnd);
        Map<String, BigDecimal> actualDeptMap = new HashMap<>();
        for (Object[] row : actualByDept) {
            String dept = (String) row[0];
            if (dept != null) actualDeptMap.put(dept, safeDec(row[1]));
        }

        List<Map<String, Object>> deptBudgetList = new ArrayList<>();
        for (MaterialBudget b : deptBudgets) {
            BigDecimal budgetAmt = safeDec(b.getBudgetAmount());
            BigDecimal actualAmt = actualDeptMap.getOrDefault(b.getTargetCode(), ZERO);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("departmentCode", b.getTargetCode());
            item.put("departmentName", b.getTargetName());
            item.put("budgetAmount", budgetAmt);
            item.put("actualAmount", actualAmt);
            item.put("executionRate", budgetAmt.compareTo(ZERO) > 0
                    ? actualAmt.divide(budgetAmt, 4, RoundingMode.HALF_UP)
                    : ZERO);
            item.put("remainingBudget", budgetAmt.subtract(actualAmt));
            item.put("overBudget", actualAmt.compareTo(budgetAmt) > 0);
            item.put("warning", actualAmt.compareTo(budgetAmt.multiply(new BigDecimal("0.90"))) >= 0);
            deptBudgetList.add(item);
        }
        deptBudgetList.sort((a, b) -> ((BigDecimal) b.get("executionRate")).compareTo((BigDecimal) a.get("executionRate")));
        result.put("departmentBudgets", deptBudgetList);
        return result;
    }

    public Map<String, Object> saveBudget(MaterialBudget budget) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (budget.getFiscalYear() == null || budget.getBudgetType() == null) {
            result.put("error", "年度和预算类型为必填");
            return result;
        }
        Optional<MaterialBudget> existing = budgetRepository
                .findByFiscalYearAndBudgetTypeAndTargetCode(budget.getFiscalYear(), budget.getBudgetType(),
                        budget.getTargetCode() != null ? budget.getTargetCode() : "");
        MaterialBudget saved;
        if (existing.isPresent()) {
            MaterialBudget b = existing.get();
            if (budget.getBudgetAmount() != null) b.setBudgetAmount(budget.getBudgetAmount());
            if (budget.getTargetName() != null) b.setTargetName(budget.getTargetName());
            if (budget.getRemark() != null) b.setRemark(budget.getRemark());
            saved = budgetRepository.save(b);
        } else {
            if (budget.getTargetCode() == null) budget.setTargetCode("");
            saved = budgetRepository.save(budget);
        }
        result.put("data", saved);
        return result;
    }

    public List<MaterialBudget> listBudgets(String fiscalYear, String budgetType) {
        if (budgetType != null && !budgetType.isBlank()) {
            return budgetRepository.findByFiscalYearAndBudgetType(fiscalYear, budgetType);
        }
        return budgetRepository.findByFiscalYear(fiscalYear);
    }

    public Map<String, Object> getMultiDimensionReport(String timeDimension, String startDate, String endDate,
                                                        String category, String department, String warehouseCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime start = startDate != null ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : LocalDateTime.now();

        List<StockTransaction> allTxs = transactionRepository.findAll();
        List<StockBalance> allBalances = balanceRepository.findAll();
        Map<Long, Material> materialMap = new HashMap<>();
        for (Material m : materialRepository.findAll()) materialMap.put(m.getId(), m);

        List<Map<String, Object>> filteredTxs = new ArrayList<>();
        for (StockTransaction tx : allTxs) {
            if (tx.getTransactionTime() != null && (tx.getTransactionTime().isBefore(start) || tx.getTransactionTime().isAfter(end)))
                continue;
            if (category != null && !category.isBlank()) {
                Material m = materialMap.get(tx.getMaterialId());
                if (m == null || !category.equals(m.getCategory())) continue;
            }
            if (department != null && !department.isBlank()) {
                if (!department.equals(tx.getDepartment())) continue;
            }
            if (warehouseCode != null && !warehouseCode.isBlank()) {
                if (!warehouseCode.equals(tx.getWarehouseCode())) continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            String timeKey;
            if ("day".equals(timeDimension)) {
                timeKey = tx.getTransactionTime().toLocalDate().toString();
            } else if ("quarter".equals(timeDimension)) {
                YearMonth ym = YearMonth.from(tx.getTransactionTime());
                int q = (ym.getMonthValue() - 1) / 3 + 1;
                timeKey = ym.getYear() + "-Q" + q;
            } else if ("year".equals(timeDimension)) {
                timeKey = String.valueOf(tx.getTransactionTime().getYear());
            } else {
                timeKey = YearMonth.from(tx.getTransactionTime()).toString();
            }
            row.put("timePeriod", timeKey);
            row.put("materialId", tx.getMaterialId());
            row.put("materialCode", tx.getMaterialCode());
            row.put("materialName", tx.getMaterialName());
            Material m = materialMap.get(tx.getMaterialId());
            row.put("category", m != null ? m.getCategory() : tx.getMaterialId());
            row.put("warehouseCode", tx.getWarehouseCode());
            row.put("department", tx.getDepartment());
            row.put("transactionType", tx.getType());
            row.put("quantity", tx.getQuantity());
            row.put("unitPrice", tx.getUnitPrice());
            row.put("totalAmount", safeDec(tx.getTotalAmount()));
            filteredTxs.add(row);
        }

        Map<String, Map<String, Object>> aggMap = new LinkedHashMap<>();
        for (Map<String, Object> row : filteredTxs) {
            String key = row.get("timePeriod") + "|" + row.get("category") + "|"
                    + row.get("department") + "|" + row.get("warehouseCode");
            Map<String, Object> agg = aggMap.computeIfAbsent(key, k -> {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("timePeriod", row.get("timePeriod"));
                a.put("category", row.get("category"));
                a.put("department", row.get("department"));
                a.put("warehouseCode", row.get("warehouseCode"));
                a.put("inboundQty", ZERO);
                a.put("inboundAmount", ZERO);
                a.put("outboundQty", ZERO);
                a.put("outboundAmount", ZERO);
                return a;
            });
            boolean isIn = ((String) row.get("transactionType")).contains("入库");
            BigDecimal qty = safeDec(row.get("quantity"));
            BigDecimal amt = safeDec(row.get("totalAmount"));
            if (isIn) {
                agg.put("inboundQty", ((BigDecimal) agg.get("inboundQty")).add(qty));
                agg.put("inboundAmount", ((BigDecimal) agg.get("inboundAmount")).add(amt));
            } else {
                agg.put("outboundQty", ((BigDecimal) agg.get("outboundQty")).add(qty));
                agg.put("outboundAmount", ((BigDecimal) agg.get("outboundAmount")).add(amt));
            }
        }
        result.put("aggregations", new ArrayList<>(aggMap.values()));
        result.put("details", filteredTxs);

        BigDecimal totalIn = ZERO, totalOut = ZERO;
        for (Map<String, Object> a : aggMap.values()) {
            totalIn = totalIn.add((BigDecimal) a.get("inboundAmount"));
            totalOut = totalOut.add((BigDecimal) a.get("outboundAmount"));
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalInboundAmount", totalIn);
        summary.put("totalOutboundAmount", totalOut);
        summary.put("netChange", totalIn.subtract(totalOut));
        summary.put("recordCount", filteredTxs.size());
        result.put("summary", summary);
        return result;
    }

    private BigDecimal safeDec(Object o) {
        if (o == null) return ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return ZERO;
        }
    }

    @lombok.Data
    public static class MaterialAbcItem {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String category;
        private BigDecimal amount;
        private BigDecimal ratio;
        private BigDecimal cumulativeRatio;
        private String categoryClass;

        public MaterialAbcItem(Long materialId, String materialCode, String materialName,
                               String category, BigDecimal amount) {
            this.materialId = materialId;
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.category = category;
            this.amount = amount;
        }
    }
}

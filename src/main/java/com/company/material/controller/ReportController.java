package com.company.material.controller;

import com.company.material.entity.MaterialBudget;
import com.company.material.service.CostAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CostAnalysisService costAnalysisService;

    private static final List<String> MANAGEMENT_ROLES = Arrays.asList("管理层", "系统管理员", "财务主管", "ADMIN", "MANAGER");

    private ResponseEntity<?> checkManagementRole(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未认证"));
        }
        for (String mr : MANAGEMENT_ROLES) {
            if (mr.equalsIgnoreCase(role) || role.contains(mr)) {
                return null;
            }
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "该接口需要管理层权限，当前角色：" + role));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpServletRequest request) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getDashboard();
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("data", emptyDashboard(), "warning", e.getMessage()));
        }
    }

    private Map<String, Object> emptyDashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalInventoryValue", 0);
        m.put("monthInboundAmount", 0);
        m.put("monthOutboundAmount", 0);
        m.put("monthPurchaseAmount", 0);
        m.put("inventoryTurnoverRate", 0);
        m.put("stagnantMaterialAmount", 0);
        m.put("safetyStockWarningCount", 0);
        return m;
    }

    @GetMapping("/abc-analysis")
    public ResponseEntity<?> getAbcAnalysis(HttpServletRequest request) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getAbcAnalysis();
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("totalMaterialCount", 0);
            empty.put("totalAnnualOutboundAmount", 0);
            empty.put("classA", emptyAbcClass("A", "重点管理"));
            empty.put("classB", emptyAbcClass("B", "常规管理"));
            empty.put("classC", emptyAbcClass("C", "简化管理"));
            empty.put("allItems", Collections.emptyList());
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }

    private Map<String, Object> emptyAbcClass(String cls, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("class", cls);
        m.put("description", desc);
        m.put("materialCount", 0);
        m.put("countRatio", 0);
        m.put("amount", 0);
        m.put("amountRatio", 0);
        m.put("materials", Collections.emptyList());
        return m;
    }

    @GetMapping("/stagnant-analysis")
    public ResponseEntity<?> getStagnantAnalysis(HttpServletRequest request,
                                                  @RequestParam(required = false) Integer topN) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getStagnantAnalysis(topN);
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("stagnantByDays", Collections.emptyMap());
            empty.put("trend", Collections.emptyList());
            empty.put("recommendationTopN", Collections.emptyList());
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }

    @GetMapping("/turnover-analysis")
    public ResponseEntity<?> getTurnoverAnalysis(HttpServletRequest request) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getTurnoverAnalysis();
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("fastTurnoverMaterials", Collections.emptyList());
            empty.put("slowTurnoverMaterials", Collections.emptyList());
            empty.put("categoryTurnoverComparison", Collections.emptyList());
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }

    @GetMapping("/cost-trend")
    public ResponseEntity<?> getCostTrend(HttpServletRequest request) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getCostTrendAnalysis();
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("monthlyTrend", Collections.emptyList());
            empty.put("purchasePriceVolatility", Collections.emptyList());
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }

    @GetMapping("/department-expense")
    public ResponseEntity<?> getDepartmentExpense(HttpServletRequest request) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getDepartmentExpenseAnalysis();
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("monthlyRanking", Collections.emptyList());
            empty.put("departmentMonthlyTrends", Collections.emptyList());
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }

    @GetMapping("/budget-execution")
    public ResponseEntity<?> getBudgetExecution(HttpServletRequest request,
                                                 @RequestParam(required = false) String fiscalYear) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getBudgetExecution(fiscalYear);
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            Map<String, Object> pb = new LinkedHashMap<>();
            pb.put("budgetAmount", 0);
            pb.put("actualAmount", 0);
            pb.put("executionRate", 0);
            pb.put("remainingBudget", 0);
            pb.put("overBudget", false);
            pb.put("warning", false);
            empty.put("annualPurchaseBudget", pb);
            empty.put("departmentBudgets", Collections.emptyList());
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }

    @PostMapping("/budgets")
    public ResponseEntity<?> saveBudget(HttpServletRequest request, @RequestBody MaterialBudget budget) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> result = costAnalysisService.saveBudget(budget);
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/budgets")
    public ResponseEntity<?> listBudgets(HttpServletRequest request,
                                          @RequestParam String fiscalYear,
                                          @RequestParam(required = false) String budgetType) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            List<MaterialBudget> data = costAnalysisService.listBudgets(fiscalYear, budgetType);
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("data", Collections.emptyList(), "warning", e.getMessage()));
        }
    }

    @GetMapping("/multi-dimension")
    public ResponseEntity<?> getMultiDimensionReport(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "month") String timeDimension,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String warehouseCode) {
        ResponseEntity<?> forbidden = checkManagementRole(request);
        if (forbidden != null) return forbidden;
        try {
            Map<String, Object> data = costAnalysisService.getMultiDimensionReport(
                    timeDimension, startDate, endDate, category, department, warehouseCode);
            return ResponseEntity.ok(Map.of("data", data));
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("aggregations", Collections.emptyList());
            empty.put("details", Collections.emptyList());
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalInboundAmount", 0);
            summary.put("totalOutboundAmount", 0);
            summary.put("netChange", 0);
            summary.put("recordCount", 0);
            empty.put("summary", summary);
            return ResponseEntity.ok(Map.of("data", empty, "warning", e.getMessage()));
        }
    }
}

package com.company.material.config;

import com.company.material.entity.*;
import com.company.material.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final StockTransactionRepository transactionRepository;
    private final StockBalanceRepository balanceRepository;
    private final MaterialBudgetRepository budgetRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createUser("admin", "admin123", "系统管理员", "信息中心", "管理员");
            createUser("manager", "manager123", "陈总", "管理层", "管理层");
            createUser("finance", "finance123", "周财务", "财务部", "财务主管");
            createUser("wzhang", "123456", "张伟", "采购部", "采购员");
            createUser("limei", "123456", "李梅", "仓储部", "库管员");
            createUser("wangq", "123456", "王强", "生产部", "普通员工");
            createUser("zhaoli", "123456", "赵丽", "生产部", "普通员工");
            createUser("sunhua", "123456", "孙华", "设备部", "普通员工");
        }

        if (warehouseRepository.count() == 0) {
            createWarehouse("WH001", "原料一号库", "厂区东北角", "李梅", "13800001111");
            createWarehouse("WH002", "成品库", "厂区南门", "赵刚", "13800002222");
            createWarehouse("WH003", "备件库", "维修车间旁", "孙丽", "13800003333");
        }

        if (supplierRepository.count() == 0) {
            createSupplier("SUP001", "华东钢铁有限公司", "陈经理", "021-66668888", "上海市宝山区", "原材料");
            createSupplier("SUP002", "精密轴承制造厂", "刘主管", "0510-88889999", "江苏省无锡市", "机械备件");
            createSupplier("SUP003", "环球电气设备公司", "周工", "020-77776666", "广东省广州市", "电气设备");
        }

        if (materialRepository.count() == 0) {
            createMaterial("MAT0001", "热轧钢板", "原材料", "吨", "Q235B 10mm", new BigDecimal("4200.00"), 50);
            createMaterial("MAT0002", "深沟球轴承", "机械备件", "个", "6206-2RS", new BigDecimal("35.50"), 200);
            createMaterial("MAT0003", "三相异步电机", "电气设备", "台", "Y2-132M-4 7.5kW", new BigDecimal("1850.00"), 10);
            createMaterial("MAT0004", "液压油", "辅料", "桶", "L-HM46 200L", new BigDecimal("980.00"), 30);
            createMaterial("MAT0005", "劳保手套", "低值易耗", "副", "丁腈防滑", new BigDecimal("8.50"), 500);
            createMaterial("MAT0006", "圆钢", "原材料", "吨", "45# Φ50mm", new BigDecimal("4800.00"), 30);
            createMaterial("MAT0007", "PLC控制器", "电气设备", "台", "S7-1200", new BigDecimal("3200.00"), 5);
            createMaterial("MAT0008", "密封垫片", "机械备件", "个", "Φ100橡胶", new BigDecimal("2.80"), 1000);
        }

        if (balanceRepository.count() == 0 && materialRepository.count() > 0 && warehouseRepository.count() > 0) {
            initSampleStockData();
        }

        if (budgetRepository.count() == 0) {
            initSampleBudgets();
        }
    }

    private void initSampleStockData() {
        Material m1 = materialRepository.findByMaterialCode("MAT0001").orElse(null);
        Material m2 = materialRepository.findByMaterialCode("MAT0002").orElse(null);
        Material m3 = materialRepository.findByMaterialCode("MAT0003").orElse(null);
        Material m4 = materialRepository.findByMaterialCode("MAT0004").orElse(null);
        Material m5 = materialRepository.findByMaterialCode("MAT0005").orElse(null);
        Material m6 = materialRepository.findByMaterialCode("MAT0006").orElse(null);
        Material m7 = materialRepository.findByMaterialCode("MAT0007").orElse(null);
        Material m8 = materialRepository.findByMaterialCode("MAT0008").orElse(null);
        Warehouse wh1 = warehouseRepository.findByWarehouseCode("WH001").orElse(null);
        Warehouse wh2 = warehouseRepository.findByWarehouseCode("WH002").orElse(null);
        Warehouse wh3 = warehouseRepository.findByWarehouseCode("WH003").orElse(null);

        if (m1 != null && wh1 != null) createBalance(m1, wh1, new BigDecimal("120.5"), new BigDecimal("4250.00"), LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(2));
        if (m2 != null && wh3 != null) createBalance(m2, wh3, new BigDecimal("580"), new BigDecimal("35.50"), LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        if (m3 != null && wh3 != null) createBalance(m3, wh3, new BigDecimal("25"), new BigDecimal("1880.00"), LocalDateTime.now().minusDays(15), LocalDateTime.now().minusDays(3));
        if (m4 != null && wh1 != null) createBalance(m4, wh1, new BigDecimal("45"), new BigDecimal("980.00"), LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(7));
        if (m5 != null && wh2 != null) createBalance(m5, wh2, new BigDecimal("1200"), new BigDecimal("8.50"), LocalDateTime.now().minusDays(3), LocalDateTime.now().minusHours(5));
        if (m6 != null && wh1 != null) createBalance(m6, wh1, new BigDecimal("85.3"), new BigDecimal("4780.00"), LocalDateTime.now().minusDays(8), LocalDateTime.now().minusDays(4));
        if (m7 != null && wh3 != null) createBalance(m7, wh3, new BigDecimal("8"), new BigDecimal("3150.00"), LocalDateTime.now().minusMonths(4), LocalDateTime.now().minusMonths(4));
        if (m8 != null && wh3 != null) createBalance(m8, wh3, new BigDecimal("3500"), new BigDecimal("2.80"), LocalDateTime.now().minusDays(12), LocalDateTime.now().minusDays(6));

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.from(now.toLocalDate()).minusMonths(i);
            int dayInMonth;
            if (i == 0) {
                dayInMonth = Math.max(1, Math.min(ym.lengthOfMonth(), now.getDayOfMonth() - 3));
            } else {
                dayInMonth = 5 + (int)(Math.random() * 10);
            }
            LocalDateTime baseTime = ym.atDay(dayInMonth).atTime(10, 0, 0);
            int outOffset1 = Math.min(2, ym.lengthOfMonth() - dayInMonth - 1);
            int outOffset2 = Math.min(3, ym.lengthOfMonth() - dayInMonth - 1);
            int outOffset3 = Math.min(5, ym.lengthOfMonth() - dayInMonth - 1);
            int outOffset4 = Math.min(1, ym.lengthOfMonth() - dayInMonth - 1);
            if (outOffset1 < 1) outOffset1 = 1;
            if (outOffset2 < 1) outOffset2 = 1;
            if (outOffset3 < 1) outOffset3 = 1;
            if (outOffset4 < 1) outOffset4 = 1;

            if (m1 != null && wh1 != null) {
                createTx("采购入库", m1, wh1, new BigDecimal("30").add(new BigDecimal(Math.random() * 20)),
                        new BigDecimal("4200").add(new BigDecimal(Math.random() * 200)), baseTime,
                        "采购部", 1L, "华东钢铁有限公司");
                createTx("领用出库", m1, wh1, new BigDecimal("20").add(new BigDecimal(Math.random() * 15)),
                        new BigDecimal("4250"), baseTime.plusDays(outOffset1),
                        "生产部", null, null);
            }
            if (m2 != null && wh3 != null) {
                createTx("采购入库", m2, wh3, new BigDecimal("200").add(new BigDecimal(Math.random() * 100)),
                        new BigDecimal("35").add(new BigDecimal(Math.random() * 3)), baseTime,
                        "采购部", 2L, "精密轴承制造厂");
                createTx("领用出库", m2, wh3, new BigDecimal("150").add(new BigDecimal(Math.random() * 80)),
                        new BigDecimal("35.50"), baseTime.plusDays(outOffset2),
                        "设备部", null, null);
            }
            if (m3 != null && wh3 != null) {
                createTx("采购入库", m3, wh3, new BigDecimal("5"),
                        new BigDecimal("1850").add(new BigDecimal(Math.random() * 100)), baseTime,
                        "采购部", 3L, "环球电气设备公司");
                createTx("领用出库", m3, wh3, new BigDecimal("3"),
                        new BigDecimal("1880"), baseTime.plusDays(outOffset3),
                        "设备部", null, null);
            }
            if (m5 != null && wh2 != null) {
                createTx("采购入库", m5, wh2, new BigDecimal("500"),
                        new BigDecimal("8.50").add(new BigDecimal(Math.random())), baseTime,
                        "采购部", null, null);
                createTx("领用出库", m5, wh2, new BigDecimal("300").add(new BigDecimal(Math.random() * 200)),
                        new BigDecimal("8.50"), baseTime.plusDays(outOffset4),
                        "生产部", null, null);
            }
        }
    }

    private void createBalance(Material m, Warehouse wh, BigDecimal qty, BigDecimal avgCost,
                                LocalDateTime lastIn, LocalDateTime lastOut) {
        StockBalance b = new StockBalance();
        b.setMaterialId(m.getId());
        b.setMaterialCode(m.getMaterialCode());
        b.setMaterialName(m.getName());
        b.setCategory(m.getCategory());
        b.setWarehouseId(wh.getId());
        b.setWarehouseCode(wh.getWarehouseCode());
        b.setQuantity(qty);
        b.setAvgCost(avgCost);
        b.setTotalAmount(qty.multiply(avgCost));
        b.setLastInTime(lastIn);
        b.setLastOutTime(lastOut);
        balanceRepository.save(b);
    }

    private void createTx(String type, Material m, Warehouse wh, BigDecimal qty, BigDecimal unitPrice,
                           LocalDateTime time, String dept, Long supplierId, String supplierName) {
        StockTransaction tx = new StockTransaction();
        String prefix = type.contains("入库") ? "RK" : "CK";
        tx.setTransactionNo(prefix + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        tx.setType(type);
        tx.setMaterialId(m.getId());
        tx.setMaterialCode(m.getMaterialCode());
        tx.setMaterialName(m.getName());
        tx.setWarehouseId(wh.getId());
        tx.setWarehouseCode(wh.getWarehouseCode());
        tx.setQuantity(qty);
        tx.setUnitPrice(unitPrice);
        tx.setTotalAmount(qty.multiply(unitPrice));
        tx.setDepartment(dept);
        tx.setSupplierId(supplierId);
        tx.setSupplierName(supplierName);
        tx.setOperator("system");
        tx.setTransactionTime(time);
        transactionRepository.save(tx);
    }

    private void initSampleBudgets() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        MaterialBudget totalPurchase = new MaterialBudget();
        totalPurchase.setFiscalYear(year);
        totalPurchase.setBudgetType("年度采购总预算");
        totalPurchase.setTargetCode("TOTAL");
        totalPurchase.setTargetName("年度采购总预算");
        totalPurchase.setBudgetAmount(new BigDecimal("2000000.00"));
        budgetRepository.save(totalPurchase);

        String[] depts = {"生产部", "设备部", "采购部"};
        BigDecimal[] amounts = {new BigDecimal("800000.00"), new BigDecimal("500000.00"), new BigDecimal("300000.00")};
        for (int i = 0; i < depts.length; i++) {
            MaterialBudget db = new MaterialBudget();
            db.setFiscalYear(year);
            db.setBudgetType("部门用料预算");
            db.setTargetCode(depts[i]);
            db.setTargetName(depts[i] + "年度用料预算");
            db.setBudgetAmount(amounts[i]);
            budgetRepository.save(db);
        }
    }

    private void createUser(String username, String password, String realName, String dept, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(realName);
        u.setDepartment(dept);
        u.setRole(role);
        userRepository.save(u);
    }

    private void createWarehouse(String code, String name, String location, String manager, String phone) {
        Warehouse w = new Warehouse();
        w.setWarehouseCode(code);
        w.setName(name);
        w.setLocation(location);
        w.setManager(manager);
        w.setPhone(phone);
        warehouseRepository.save(w);
    }

    private void createSupplier(String code, String name, String contact, String phone, String address, String category) {
        Supplier s = new Supplier();
        s.setSupplierCode(code);
        s.setName(name);
        s.setContactPerson(contact);
        s.setPhone(phone);
        s.setAddress(address);
        s.setCategory(category);
        supplierRepository.save(s);
    }

    private void createMaterial(String code, String name, String category, String unit, String spec, BigDecimal price, int safety) {
        Material m = new Material();
        m.setMaterialCode(code);
        m.setName(name);
        m.setCategory(category);
        m.setUnit(unit);
        m.setSpecification(spec);
        m.setReferencePrice(price);
        m.setSafetyStock(safety);
        materialRepository.save(m);
    }
}

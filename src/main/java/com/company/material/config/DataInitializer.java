package com.company.material.config;

import com.company.material.entity.Material;
import com.company.material.entity.Supplier;
import com.company.material.entity.User;
import com.company.material.entity.Warehouse;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.SupplierRepository;
import com.company.material.repository.UserRepository;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createUser("admin", "admin123", "系统管理员", "信息中心", "管理员");
            createUser("wzhang", "123456", "张伟", "采购部", "采购员");
            createUser("limei", "123456", "李梅", "仓储部", "库管员");
            createUser("wangq", "123456", "王强", "生产部", "普通员工");
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

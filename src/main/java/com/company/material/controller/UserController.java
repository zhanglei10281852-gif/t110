package com.company.material.controller;

import com.company.material.entity.User;
import com.company.material.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private boolean notAdmin(String role) {
        return !"管理员".equals(role);
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestAttribute("role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String department) {
        if (notAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限，仅管理员可访问"));
        }
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result;
        if (roleFilter != null && !roleFilter.isBlank()) {
            result = userRepository.findByRole(roleFilter, pr);
        } else if (department != null && !department.isBlank()) {
            result = userRepository.findByDepartment(department, pr);
        } else {
            result = userRepository.findAll(pr);
        }
        result.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            u.setPassword(null);
            return ResponseEntity.ok((Object) u);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestAttribute("role") String role, @RequestBody Map<String, String> body) {
        if (notAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限，仅管理员可创建用户"));
        }
        String username = body.get("username");
        if (username == null || body.get("password") == null || body.get("realName") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名、密码、姓名不能为空"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "用户名已存在"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(body.get("password")));
        user.setRealName(body.get("realName"));
        user.setDepartment(body.get("department"));
        user.setPhone(body.get("phone"));
        user.setRole(body.getOrDefault("role", "普通员工"));
        User saved = userRepository.save(user);
        saved.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestAttribute("role") String role, @PathVariable Long id, @RequestBody Map<String, String> body) {
        if (notAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        return userRepository.findById(id).map(u -> {
            if (body.get("realName") != null) u.setRealName(body.get("realName"));
            if (body.get("department") != null) u.setDepartment(body.get("department"));
            if (body.get("phone") != null) u.setPhone(body.get("phone"));
            if (body.get("role") != null) u.setRole(body.get("role"));
            if (body.get("status") != null) u.setStatus(body.get("status"));
            if (body.get("password") != null && !body.get("password").isBlank()) {
                u.setPassword(passwordEncoder.encode(body.get("password")));
            }
            User saved = userRepository.save(u);
            saved.setPassword(null);
            return ResponseEntity.ok((Object) saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@RequestAttribute("role") String role, @PathVariable Long id, @RequestBody Map<String, String> body) {
        if (notAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        String status = body.get("status");
        if (!"启用".equals(status) && !"禁用".equals(status)) {
            return ResponseEntity.badRequest().body(Map.of("error", "状态值无效"));
        }
        return userRepository.findById(id).map(u -> {
            u.setStatus(status);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "状态更新成功", "status", status));
        }).orElse(ResponseEntity.notFound().build());
    }
}

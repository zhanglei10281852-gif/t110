package com.company.material.controller;

import com.company.material.entity.User;
import com.company.material.repository.UserRepository;
import com.company.material.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户名或密码错误"));
        }
        if (!"启用".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "账号已被禁用"));
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        userInfo.put("department", user.getDepartment());
        result.put("user", userInfo);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        if (username == null || password == null || realName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名、密码、姓名不能为空"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "用户名已存在"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setDepartment(body.get("department"));
        user.setPhone(body.get("phone"));
        user.setRole(body.getOrDefault("role", "普通员工"));
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "message", "注册成功"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute("userId") Long userId) {
        return userRepository.findById(userId).map(u -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", u.getId());
            info.put("username", u.getUsername());
            info.put("realName", u.getRealName());
            info.put("role", u.getRole());
            info.put("department", u.getDepartment());
            info.put("phone", u.getPhone());
            info.put("lastLoginAt", u.getLastLoginAt());
            return ResponseEntity.ok((Object) info);
        }).orElse(ResponseEntity.notFound().build());
    }
}

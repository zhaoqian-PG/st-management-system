package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.model.User;
import com.stmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private BCryptPasswordEncoder encoder;

    @PostConstruct
    public void init() { encoder = new BCryptPasswordEncoder(); }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("ユーザー名とパスワードを入力してください", 401));
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (!opt.isPresent() || !encoder.matches(password, opt.get().getPassword())) {
            return ResponseEntity.status(401).body(ApiResponse.error("ユーザー名またはパスワードが間違っています", 401));
        }

        User user = opt.get();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("employeeId", user.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(data, "ログイン成功"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestParam String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (!opt.isPresent()) return ResponseEntity.status(404).body(ApiResponse.error("ユーザーが見つかりません", 404));
        User user = opt.get();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("employeeId", user.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

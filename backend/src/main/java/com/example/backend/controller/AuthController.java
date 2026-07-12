package com.example.backend.controller;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.UpdateProfileRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.AuthResponse;
import com.example.backend.entity.User;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng ký tài khoản thành công!"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công!"));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<User>> getProfile(@PathVariable Long userId) {
        User user = authService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(user, "Lấy thông tin cá nhân thành công!"));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequest request) {
        User updatedUser = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser, "Cập nhật thông tin thành công!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công!", "Đã đăng xuất"));
    }
}

// Feature Implementation: code api đăng ký tài khoản

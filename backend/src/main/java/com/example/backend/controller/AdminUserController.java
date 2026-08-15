package com.example.backend.controller;

import com.example.backend.dto.request.AdminUserRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.Cart;
import com.example.backend.entity.User;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    // Get all users (Owner / Admin view)
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        
        List<User> users = userRepository.findAll();

        if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role.trim())) {
            users = users.stream()
                    .filter(u -> role.trim().equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String kw = search.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)) ||
                            (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw)) ||
                            (u.getPhone() != null && u.getPhone().contains(kw)))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(ApiResponse.ok(users, "Lấy danh sách người dùng thành công!"));
    }

    // Owner: Create new Admin or User account
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody AdminUserRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không được để trống!");
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trên hệ thống!");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && userRepository.existsByEmail(request.getEmail().trim())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        String targetRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "ROLE_ADMIN";
        if (!targetRole.startsWith("ROLE_")) {
            targetRole = "ROLE_" + targetRole;
        }

        if ("ROLE_OWNER".equalsIgnoreCase(targetRole)) {
            throw new RuntimeException("Không thể tạo thêm tài khoản Chủ Sở Hữu (Owner)! Vai trò Owner là duy nhất cho tài khoản hệ thống gốc.");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(request.getPassword() != null && !request.getPassword().trim().isEmpty() ? request.getPassword().trim() : "123456")
                .email(request.getEmail() != null ? request.getEmail().trim() : request.getUsername().trim() + "@fsshop.com")
                .fullName(request.getFullName() != null ? request.getFullName().trim() : request.getUsername().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : "")
                .address(request.getAddress() != null ? request.getAddress().trim() : "")
                .role(targetRole)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .build();

        User saved = userRepository.save(user);

        // Auto create empty cart for the new account
        Cart cart = Cart.builder().user(saved).build();
        cartRepository.save(cart);

        return ResponseEntity.ok(ApiResponse.ok(saved, "Tạo tài khoản mới thành công!"));
    }

    // Owner: Change User Role (ROLE_USER, ROLE_ADMIN)
    @PutMapping("/{userId}/role")
    @Transactional
    public ResponseEntity<ApiResponse<User>> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        if ("owner".equalsIgnoreCase(user.getUsername()) || "ROLE_OWNER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Tài khoản Chủ Sở Hữu Tối Cao (Root Owner) là cố định, không thể thay đổi vai trò!");
        }

        String cleanRole = role != null ? role.trim().toUpperCase() : "ROLE_USER";
        if (!cleanRole.startsWith("ROLE_")) {
            cleanRole = "ROLE_" + cleanRole;
        }

        if ("ROLE_OWNER".equalsIgnoreCase(cleanRole)) {
            throw new RuntimeException("Không thể nâng cấp tài khoản khác thành Chủ Sở Hữu (Owner)!");
        }

        user.setRole(cleanRole);
        User updated = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(updated, "Cập nhật vai trò tài khoản thành công!"));
    }

    // Owner: Change User Status (1 = Active, 0 = Locked)
    @PutMapping("/{userId}/status")
    @Transactional
    public ResponseEntity<ApiResponse<User>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        if ("owner".equalsIgnoreCase(user.getUsername()) || "ROLE_OWNER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Tài khoản Chủ Sở Hữu Tối Cao (Root Owner) là cố định, không thể bị khóa!");
        }

        user.setStatus(status != null ? status : 1);
        User updated = userRepository.save(user);

        String msg = (user.getStatus() == 1) ? "Đã mở khóa tài khoản thành công!" : "Đã khóa tài khoản thành công!";
        return ResponseEntity.ok(ApiResponse.ok(updated, msg));
    }
}

// Feature Implementation: thêm tính năng quản lý tài khoản người dùng cho admin

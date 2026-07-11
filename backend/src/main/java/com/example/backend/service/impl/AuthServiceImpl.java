package com.example.backend.service.impl;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.UpdateProfileRequest;
import com.example.backend.dto.response.AuthResponse;
import com.example.backend.entity.Cart;
import com.example.backend.entity.User;
import com.example.backend.entity.UserAddress;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.UserAddressRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final UserAddressRepository userAddressRepository;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role("ROLE_USER")
                .status(1)
                .build();

        User savedUser = userRepository.save(user);

        // Tự tạo giỏ hàng rỗng cho người dùng mới
        Cart cart = Cart.builder()
                .user(savedUser)
                .build();
        cartRepository.save(cart);

        // Tự tạo địa chỉ mặc định nếu có nhập địa chỉ khi đăng ký
        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            UserAddress defaultAddress = UserAddress.builder()
                    .user(savedUser)
                    .recipientName(savedUser.getFullName() != null && !savedUser.getFullName().trim().isEmpty() ? savedUser.getFullName() : savedUser.getUsername())
                    .recipientPhone(savedUser.getPhone() != null ? savedUser.getPhone() : "")
                    .detailAddress(request.getAddress().trim())
                    .fullAddress(request.getAddress().trim())
                    .isDefault(true)
                    .build();
            userAddressRepository.save(defaultAddress);
        }

        String token = "TOKEN-" + UUID.randomUUID().toString();

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .avatar(savedUser.getAvatar())
                .phone(savedUser.getPhone())
                .address(savedUser.getAddress())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("Tài khoản đã bị khóa!");
        }

        String token = "TOKEN-" + UUID.randomUUID().toString();

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .address(user.getAddress())
                .build();
    }

    @Override
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getProfile(userId);
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        User savedUser = userRepository.save(user);

        // Tự động đồng bộ địa chỉ mặc định trong sổ địa chỉ nếu chưa có
        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIdDesc(userId);
            if (addresses.isEmpty()) {
                UserAddress defaultAddress = UserAddress.builder()
                        .user(savedUser)
                        .recipientName(savedUser.getFullName() != null && !savedUser.getFullName().trim().isEmpty() ? savedUser.getFullName() : savedUser.getUsername())
                        .recipientPhone(savedUser.getPhone() != null ? savedUser.getPhone() : "")
                        .detailAddress(request.getAddress().trim())
                        .fullAddress(request.getAddress().trim())
                        .isDefault(true)
                        .build();
                userAddressRepository.save(defaultAddress);
            }
        }

        return savedUser;
    }
}

// Feature Implementation: viết api đăng nhập và phân quyền

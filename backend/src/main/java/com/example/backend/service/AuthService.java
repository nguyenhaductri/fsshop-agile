package com.example.backend.service;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.UpdateProfileRequest;
import com.example.backend.dto.response.AuthResponse;
import com.example.backend.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User getProfile(Long userId);

    User updateProfile(Long userId, UpdateProfileRequest request);
}

// Feature Implementation: api hủy session/token (đăng xuất)

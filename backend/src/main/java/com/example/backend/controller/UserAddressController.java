package com.example.backend.controller;

import com.example.backend.dto.request.UserAddressRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.UserAddressResponse;
import com.example.backend.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getAddressesByUser(@PathVariable Long userId) {
        List<UserAddressResponse> addresses = userAddressService.getAddressesByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(addresses, "Lấy sổ địa chỉ thành công!"));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserAddressResponse>> createAddress(
            @PathVariable Long userId,
            @RequestBody UserAddressRequest request) {
        UserAddressResponse address = userAddressService.createAddress(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(address, "Thêm địa chỉ mới thành công!"));
    }

    @PutMapping("/{userId}/{addressId}")
    public ResponseEntity<ApiResponse<UserAddressResponse>> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @RequestBody UserAddressRequest request) {
        UserAddressResponse address = userAddressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(ApiResponse.ok(address, "Cập nhật địa chỉ thành công!"));
    }

    @DeleteMapping("/{userId}/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        userAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã xóa địa chỉ!"));
    }

    @PutMapping("/{userId}/{addressId}/default")
    public ResponseEntity<ApiResponse<UserAddressResponse>> setDefaultAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        UserAddressResponse address = userAddressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok(address, "Đã đặt làm địa chỉ mặc định!"));
    }
}

// Feature Implementation: api upload avatar, sửa địa chỉ

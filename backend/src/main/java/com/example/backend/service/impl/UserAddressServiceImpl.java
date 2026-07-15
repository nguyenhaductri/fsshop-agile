package com.example.backend.service.impl;

import com.example.backend.dto.request.UserAddressRequest;
import com.example.backend.dto.response.UserAddressResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.UserAddress;
import com.example.backend.repository.UserAddressRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    @Override
    public List<UserAddressResponse> getAddressesByUser(Long userId) {
        return userAddressRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::mapToUserAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressResponse createAddress(Long userId, UserAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        List<UserAddress> existingAddresses = userAddressRepository.findByUserIdOrderByIdDesc(userId);
        boolean isDefault = existingAddresses.isEmpty() || Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault && !existingAddresses.isEmpty()) {
            clearDefaultAddress(userId);
        }

        String fullAddress = buildFullAddress(request.getDetailAddress(), request.getWard(), request.getDistrict(), request.getProvince());

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .detailAddress(request.getDetailAddress())
                .fullAddress(fullAddress)
                .isDefault(isDefault)
                .build();

        UserAddress saved = userAddressRepository.save(address);
        return mapToUserAddressResponse(saved);
    }

    @Override
    @Transactional
    public UserAddressResponse updateAddress(Long userId, Long addressId, UserAddressRequest request) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại!"));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress(userId);
            address.setIsDefault(true);
        }

        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetailAddress(request.getDetailAddress());
        address.setFullAddress(buildFullAddress(request.getDetailAddress(), request.getWard(), request.getDistrict(), request.getProvince()));

        UserAddress updated = userAddressRepository.save(address);
        return mapToUserAddressResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại!"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);

        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepository.findByUserIdOrderByIdDesc(userId);
            if (!remaining.isEmpty()) {
                UserAddress first = remaining.get(0);
                first.setIsDefault(true);
                userAddressRepository.save(first);
            }
        }
    }

    @Override
    @Transactional
    public UserAddressResponse setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại!"));

        clearDefaultAddress(userId);
        address.setIsDefault(true);
        UserAddress saved = userAddressRepository.save(address);
        return mapToUserAddressResponse(saved);
    }

    private void clearDefaultAddress(Long userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
            addr.setIsDefault(false);
            userAddressRepository.save(addr);
        });
    }

    private String buildFullAddress(String detail, String ward, String district, String province) {
        StringBuilder sb = new StringBuilder();
        if (detail != null && !detail.trim().isEmpty()) sb.append(detail.trim());
        if (ward != null && !ward.trim().isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(ward.trim());
        if (district != null && !district.trim().isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(district.trim());
        if (province != null && !province.trim().isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(province.trim());
        return sb.toString();
    }

    private UserAddressResponse mapToUserAddressResponse(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}

// Feature Implementation: sửa logic cập nhật biến thể và đồng bộ địa chỉ nhận hàng

package com.example.backend.service;

import com.example.backend.dto.request.UserAddressRequest;
import com.example.backend.dto.response.UserAddressResponse;

import java.util.List;

public interface UserAddressService {

    List<UserAddressResponse> getAddressesByUser(Long userId);

    UserAddressResponse createAddress(Long userId, UserAddressRequest request);

    UserAddressResponse updateAddress(Long userId, Long addressId, UserAddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    UserAddressResponse setDefaultAddress(Long userId, Long addressId);
}

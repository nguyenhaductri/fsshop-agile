package com.example.backend.service;

import com.example.backend.dto.request.AddToCartRequest;
import com.example.backend.dto.request.UpdateCartItemRequest;
import com.example.backend.dto.response.CartResponse;

public interface CartService {

    CartResponse getCartByUser(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}

// Feature Implementation: ui/api xóa item khỏi giỏ

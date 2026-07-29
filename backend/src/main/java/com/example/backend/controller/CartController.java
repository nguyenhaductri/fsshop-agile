package com.example.backend.controller;

import com.example.backend.dto.request.AddToCartRequest;
import com.example.backend.dto.request.UpdateCartItemRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CartResponse;
import com.example.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCartByUser(@PathVariable Long userId) {
        CartResponse response = cartService.getCartByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy thông tin giỏ hàng thành công!"));
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable Long userId,
            @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Thêm sản phẩm vào giỏ hàng thành công!"));
    }

    @PutMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateCartItem(userId, cartItemId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật số lượng giỏ hàng thành công!"));
    }

    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {
        CartResponse response = cartService.removeCartItem(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đã xóa sản phẩm khỏi giỏ hàng!"));
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã dọn sạch giỏ hàng!"));
    }
}

// Feature Implementation: api check tồn & add cart db

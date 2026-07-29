package com.example.backend.controller;

import com.example.backend.dto.request.AddToCartRequest;
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

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCartByUserId(userId)));
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(cartService.updateItemQuantity(userId, itemId, quantity)));
    }
}


    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@RequestParam Long userId, @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addToCart(userId, request)));
    }
}

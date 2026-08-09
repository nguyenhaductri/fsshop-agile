package com.example.backend.controller;

import com.example.backend.dto.request.CreateOrderRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.OrderResponse;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{userId}/create")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @PathVariable Long userId,
            @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đặt hàng thành công!"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByUser(@PathVariable Long userId) {
        List<OrderResponse> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(orders, "Lấy danh sách đơn hàng thành công!"));
    }

    @GetMapping("/{userId}/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.getOrderDetail(orderId, userId);
        return ResponseEntity.ok(ApiResponse.ok(order, "Lấy chi tiết đơn hàng thành công!"));
    }

    @PostMapping("/{userId}/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        OrderResponse response = orderService.cancelOrder(orderId, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hủy đơn hàng thành công!"));
    }

    @PostMapping("/{userId}/{orderId}/confirm-received")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrderReceived(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        OrderResponse response = orderService.confirmOrderReceived(userId, orderId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Xác nhận nhận hàng thành công. Đơn hàng đã hoàn tất!"));
    }
}

// Feature Implementation: api lưu thông tin shipping

// Feature Implementation: api get order details

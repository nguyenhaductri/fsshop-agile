package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.OrderResponse;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status) {
        List<OrderResponse> orders = orderService.getAllOrdersAdmin(status);
        return ResponseEntity.ok(ApiResponse.ok(orders, "Lấy danh sách tất cả đơn hàng hệ thống thành công!"));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String newStatus,
            @RequestParam(required = false) String note) {
        OrderResponse response = orderService.updateOrderStatusAdmin(orderId, newStatus, note);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật tiến trình đơn hàng thành công!"));
    }

    @PutMapping("/{orderId}/review-cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> reviewCancelOrder(
            @PathVariable Long orderId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String note) {
        OrderResponse response = orderService.adminApproveCancelOrder(orderId, approve, note);
        String msg = approve ? "Đã chấp nhận yêu cầu hủy đơn hàng!" : "Đã từ chối yêu cầu hủy đơn hàng!";
        return ResponseEntity.ok(ApiResponse.ok(response, msg));
    }
}

// Feature Implementation: ui/api update trạng thái giao

package com.example.backend.service;

import com.example.backend.dto.request.CreateOrderRequest;
import com.example.backend.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    List<OrderResponse> getOrdersByUser(Long userId);

    OrderResponse getOrderDetail(Long orderId, Long userId);

    OrderResponse cancelOrder(Long orderId, Long userId, String reason);

    OrderResponse confirmOrderReceived(Long userId, Long orderId);

    List<OrderResponse> getAllOrdersAdmin(String status);

    OrderResponse updateOrderStatusAdmin(Long orderId, String newStatus, String note);

    OrderResponse adminApproveCancelOrder(Long orderId, boolean approve, String note);
}

// Feature Implementation: api get order history

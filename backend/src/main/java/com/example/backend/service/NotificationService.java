package com.example.backend.service;

import com.example.backend.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(Long userId, String title, String message, String type, String link);

    List<NotificationResponse> getUserNotifications(Long userId);

    Long getUnreadCount(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    void notifyCartUsersIfVariantOutOfStock(Long variantId);
}

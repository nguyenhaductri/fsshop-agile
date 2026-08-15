package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.NotificationResponse;
import com.example.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(@PathVariable Long userId) {
        List<NotificationResponse> list = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(list, "Lấy danh sách thông báo thành công!"));
    }

    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(count, "Lấy số lượng thông báo chưa đọc thành công!"));
    }

    @PostMapping("/{userId}/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long userId, @PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đánh dấu thông báo là đã đọc!"));
    }

    @PostMapping("/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đánh dấu tất cả thông báo là đã đọc!"));
    }
}

package com.example.backend.service.impl;

import com.example.backend.dto.response.NotificationResponse;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Notification;
import com.example.backend.entity.ProductVariant;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(Long userId, String title, String message, String type, String link) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .link(link)
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findForUserOrderByIdDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadForUser(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> list = notificationRepository.findForUserOrderByIdDesc(userId);
        for (Notification n : list) {
            if (n.getIsRead() == null || !n.getIsRead()) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    @Override
    @Transactional
    public void notifyCartUsersIfVariantOutOfStock(Long variantId) {
        if (variantId == null) return;
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null || variant.getStockQuantity() > 0) {
            return;
        }

        List<CartItem> cartItems = cartItemRepository.findByVariantId(variantId);
        for (CartItem ci : cartItems) {
            if (ci.getCart() != null && ci.getCart().getUser() != null) {
                Long targetUserId = ci.getCart().getUser().getId();
                String prodName = variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm";
                String spec = variant.getSize() + " - " + variant.getColor();

                createNotification(
                        targetUserId,
                        "⚠️ Sản phẩm trong giỏ hàng hết hàng!",
                        "Sản phẩm '" + prodName + "' (" + spec + ") trong giỏ hàng của bạn vừa hết tồn kho!",
                        "OUT_OF_STOCK",
                        "cart"
                );
            }
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.getIsRead() != null ? n.getIsRead() : false)
                .link(n.getLink())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

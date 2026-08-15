package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // NULL = System-wide broadcast, BIGINT = Specific user

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 50)
    private String type; // 'OUT_OF_STOCK', 'VOUCHER_NEW', 'VOUCHER_DEPLETED', 'ORDER_CANCELLED'

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(length = 255)
    private String link; // Optional navigation view target: 'cart', 'orders', 'checkout'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

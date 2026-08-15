package com.example.backend.repository;

import com.example.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userId IS NULL OR n.userId = :userId ORDER BY n.id DESC")
    List<Notification> findForUserOrderByIdDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.userId IS NULL OR n.userId = :userId) AND (n.isRead = false OR n.isRead IS NULL)")
    Long countUnreadForUser(@Param("userId") Long userId);
}

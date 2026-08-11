package com.example.backend.repository;

import com.example.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByIdDesc(Long productId);

    List<Review> findByProductIdAndParentIdIsNullOrderByIdDesc(Long productId);

    List<Review> findByParentIdOrderByIdAsc(Long parentId);

    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    List<Review> findByUserIdOrderByIdDesc(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(CAST(r.ratingStars AS double)) FROM Review r WHERE r.product.id = :productId AND r.parentId IS NULL AND r.ratingStars IS NOT NULL")
    Double getAverageRatingByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.parentId IS NULL AND r.ratingStars IS NOT NULL")
    Long getReviewCountByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);
}

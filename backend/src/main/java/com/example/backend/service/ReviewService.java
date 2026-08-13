package com.example.backend.service;

import com.example.backend.dto.request.CreateReviewRequest;
import com.example.backend.dto.response.ProductRatingSummaryResponse;
import com.example.backend.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    List<ReviewResponse> getReviewsByProduct(Long productId);

    List<ReviewResponse> getReviewsByProduct(Long productId, Long currentUserId);

    ReviewResponse voteReview(Long reviewId, Long userId, String voteType);

    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    List<ReviewResponse> getReviewsByUser(Long userId);
}

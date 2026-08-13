package com.example.backend.controller;

import com.example.backend.dto.request.CreateReviewRequest;
import com.example.backend.dto.request.ReviewVoteRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductRatingSummaryResponse;
import com.example.backend.dto.response.ReviewResponse;
import com.example.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long userId,
            @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đã gửi bình luận thành công!"));
    }

    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<ApiResponse<ReviewResponse>> voteReview(
            @PathVariable Long reviewId,
            @RequestParam Long userId,
            @RequestBody ReviewVoteRequest request) {
        ReviewResponse response = reviewService.voteReview(reviewId, userId, request.getVoteType());
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật lượt thích thành công!"));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) Long userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, userId);
        return ResponseEntity.ok(ApiResponse.ok(reviews, "Lấy danh sách đánh giá sản phẩm thành công!"));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> getProductRatingSummary(@PathVariable Long productId) {
        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.ok(summary, "Lấy tóm tắt đánh giá sản phẩm thành công!"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByUser(@PathVariable Long userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(reviews, "Lấy đánh giá của tôi thành công!"));
    }
}

// Feature Implementation: api submit review

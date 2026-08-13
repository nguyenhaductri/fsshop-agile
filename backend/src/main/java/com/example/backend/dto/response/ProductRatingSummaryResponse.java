package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryResponse {
    private Double averageRating;
    private Long totalReviews;
    private Long star5Count;
    private Long star4Count;
    private Long star3Count;
    private Long star2Count;
    private Long star1Count;
}

package com.example.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {
    private Long productId;
    private Long orderId;
    private Long parentId;
    private String replyToUserName;
    private Integer ratingStars;
    private String comment;
}

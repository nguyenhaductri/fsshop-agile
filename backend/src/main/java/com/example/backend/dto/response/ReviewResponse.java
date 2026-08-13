package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long productId;
    private Long orderId;
    private Long parentId;
    private String replyToUserName;
    private Integer ratingStars;
    private String comment;
    private Long likeCount;
    private Long dislikeCount;
    private String userVote; // 'LIKE', 'DISLIKE', or null
    private List<ReviewResponse> replies;
    private LocalDateTime createdAt;
}

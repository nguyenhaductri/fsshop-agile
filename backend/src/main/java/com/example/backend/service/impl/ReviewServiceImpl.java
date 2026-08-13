package com.example.backend.service.impl;

import com.example.backend.dto.request.CreateReviewRequest;
import com.example.backend.dto.response.ProductRatingSummaryResponse;
import com.example.backend.dto.response.ReviewResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.ReviewVoteRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.NotificationService;
import com.example.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        // REPLY SUB-COMMENT LOGIC: Allow ANY logged-in user to reply to an existing comment
        if (request.getParentId() != null) {
            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập nội dung câu trả lời!");
            }

            Review targetReview = reviewRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Bình luận cần trả lời không tồn tại!"));

            User targetUser = targetReview.getUser();
            String targetUserName = (targetUser.getFullName() != null && !targetUser.getFullName().trim().isEmpty())
                    ? targetUser.getFullName()
                    : targetUser.getUsername();

            // Determine root thread ID so all sub-replies stay in the thread
            Long rootParentId = targetReview.getParentId() != null ? targetReview.getParentId() : targetReview.getId();

            Review reply = Review.builder()
                    .user(user)
                    .product(product)
                    .order(null) // Replies do not require an order
                    .parentId(rootParentId)
                    .replyToUserName(targetUserName)
                    .ratingStars(null) // Replies do not have star ratings
                    .comment(request.getComment().trim())
                    .build();

            Review savedReply = reviewRepository.save(reply);

            // Send Notification to target comment author (if not self-replying)
            if (!userId.equals(targetUser.getId())) {
                String replierName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                        ? user.getFullName()
                        : user.getUsername();
                String shortMsg = request.getComment().trim();
                if (shortMsg.length() > 60) shortMsg = shortMsg.substring(0, 60) + "...";

                notificationService.createNotification(
                        targetUser.getId(),
                        "💬 " + replierName + " vừa trả lời bình luận của bạn",
                        replierName + " đã phản hồi tại sản phẩm '" + product.getName() + "': \"" + shortMsg + "\"",
                        "REVIEW_REPLY",
                        "product-detail:" + product.getId()
                );
            }

            return mapToReviewResponse(savedReply, userId);
        }

        // ROOT REVIEW LOGIC: Only purchasers with COMPLETED orders can post root reviews
        if (request.getOrderId() == null) {
            throw new RuntimeException("Vui lòng cung cấp mã đơn hàng để gửi đánh giá gốc!");
        }

        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng tương ứng của bạn!"));

        if (!"COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá sản phẩm từ các đơn hàng đã hoàn tất (xác nhận 2 phía)!");
        }

        if (request.getRatingStars() == null || request.getRatingStars() < 1 || request.getRatingStars() > 5) {
            throw new RuntimeException("Số sao đánh giá phải từ 1 đến 5 sao!");
        }

        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, product.getId(), order.getId())) {
            throw new RuntimeException("Bạn đã gửi đánh giá cho sản phẩm này trong đơn hàng này rồi!");
        }

        String cleanComment = (request.getComment() != null) ? request.getComment().trim() : "";

        Review rootReview = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .parentId(null)
                .ratingStars(request.getRatingStars())
                .comment(cleanComment)
                .build();

        Review saved = reviewRepository.save(rootReview);
        return mapToReviewResponse(saved, userId);
    }

    @Override
    @Transactional
    public ReviewResponse voteReview(Long reviewId, Long userId, String voteType) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại!"));

        String cleanType = voteType != null ? voteType.trim().toUpperCase() : "LIKE";
        if (!"LIKE".equals(cleanType) && !"DISLIKE".equals(cleanType)) {
            cleanType = "LIKE";
        }

        Optional<ReviewVote> existingVote = reviewVoteRepository.findByReviewIdAndUserId(reviewId, userId);
        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            if (cleanType.equalsIgnoreCase(vote.getVoteType())) {
                // User clicked same vote button again -> Toggle OFF (delete vote)
                reviewVoteRepository.delete(vote);
            } else {
                // Change vote type
                vote.setVoteType(cleanType);
                reviewVoteRepository.save(vote);
            }
        } else {
            // New vote
            ReviewVote newVote = ReviewVote.builder()
                    .reviewId(reviewId)
                    .userId(userId)
                    .voteType(cleanType)
                    .build();
            reviewVoteRepository.save(newVote);
        }

        return mapToReviewResponse(review, userId);
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        return getReviewsByProduct(productId, null);
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(Long productId, Long currentUserId) {
        List<Review> rootReviews = reviewRepository.findByProductIdAndParentIdIsNullOrderByIdDesc(productId);
        return rootReviews.stream()
                .map(root -> {
                    ReviewResponse response = mapToReviewResponse(root, currentUserId);
                    // Fetch replies for this root review
                    List<Review> replies = reviewRepository.findByParentIdOrderByIdAsc(root.getId());
                    List<ReviewResponse> replyResponses = replies.stream()
                            .map(r -> mapToReviewResponse(r, currentUserId))
                            .collect(Collectors.toList());
                    response.setReplies(replyResponses);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        List<Review> rootReviews = reviewRepository.findByProductIdAndParentIdIsNullOrderByIdDesc(productId)
                .stream()
                .filter(r -> r.getRatingStars() != null)
                .collect(Collectors.toList());

        if (rootReviews.isEmpty()) {
            return ProductRatingSummaryResponse.builder()
                    .averageRating(5.0)
                    .totalReviews(0L)
                    .star5Count(0L)
                    .star4Count(0L)
                    .star3Count(0L)
                    .star2Count(0L)
                    .star1Count(0L)
                    .build();
        }

        long total = rootReviews.size();
        long s5 = rootReviews.stream().filter(r -> r.getRatingStars() == 5).count();
        long s4 = rootReviews.stream().filter(r -> r.getRatingStars() == 4).count();
        long s3 = rootReviews.stream().filter(r -> r.getRatingStars() == 3).count();
        long s2 = rootReviews.stream().filter(r -> r.getRatingStars() == 2).count();
        long s1 = rootReviews.stream().filter(r -> r.getRatingStars() == 1).count();

        double sum = rootReviews.stream().mapToInt(Review::getRatingStars).sum();
        double avg = Math.round((sum / total) * 10.0) / 10.0;

        return ProductRatingSummaryResponse.builder()
                .averageRating(avg)
                .totalReviews(total)
                .star5Count(s5)
                .star4Count(s4)
                .star3Count(s3)
                .star2Count(s2)
                .star1Count(s1)
                .build();
    }

    @Override
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(r -> mapToReviewResponse(r, userId))
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToReviewResponse(Review r, Long currentUserId) {
        User u = r.getUser();
        Long likeCount = reviewVoteRepository.countByReviewIdAndVoteType(r.getId(), "LIKE");
        Long dislikeCount = reviewVoteRepository.countByReviewIdAndVoteType(r.getId(), "DISLIKE");

        String userVote = null;
        if (currentUserId != null) {
            Optional<ReviewVote> voteOpt = reviewVoteRepository.findByReviewIdAndUserId(r.getId(), currentUserId);
            if (voteOpt.isPresent()) {
                userVote = voteOpt.get().getVoteType();
            }
        }

        String displayName = (u.getFullName() != null && !u.getFullName().trim().isEmpty())
                ? u.getFullName()
                : u.getUsername();

        return ReviewResponse.builder()
                .id(r.getId())
                .userId(u.getId())
                .userName(displayName)
                .userAvatar(u.getAvatar())
                .productId(r.getProduct().getId())
                .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
                .parentId(r.getParentId())
                .replyToUserName(r.getReplyToUserName())
                .ratingStars(r.getRatingStars())
                .comment(r.getComment())
                .likeCount(likeCount != null ? likeCount : 0L)
                .dislikeCount(dislikeCount != null ? dislikeCount : 0L)
                .userVote(userVote)
                .createdAt(r.getCreatedAt())
                .build();
    }
}

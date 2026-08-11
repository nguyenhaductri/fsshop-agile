package com.example.backend.repository;

import com.example.backend.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    Optional<ReviewVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    Long countByReviewIdAndVoteType(Long reviewId, String voteType);

    void deleteByReviewIdAndUserId(Long reviewId, Long userId);
}

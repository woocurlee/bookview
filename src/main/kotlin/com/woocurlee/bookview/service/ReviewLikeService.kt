package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.ReviewLike
import com.woocurlee.bookview.dto.LikeResponse
import com.woocurlee.bookview.repository.ReviewLikeRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class ReviewLikeService(
    private val reviewLikeRepository: ReviewLikeRepository,
    private val reviewService: ReviewService,
    private val mongoTemplate: MongoTemplate,
) {
    fun toggleLike(
        reviewNo: Long,
        googleId: String,
    ): LikeResponse {
        val review =
            reviewService.getReviewByReviewNo(reviewNo)
                ?: throw IllegalArgumentException("존재하지 않는 리뷰입니다.")

        if (review.userId == googleId) {
            throw IllegalArgumentException("본인의 리뷰에는 좋아요를 누를 수 없습니다.")
        }

        val reviewId = review.id!!
        val existing = reviewLikeRepository.findByReviewIdAndUserId(reviewId, googleId)

        return if (existing != null) {
            reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, googleId)
            incrementLikeCount(reviewId, -1)
            val updatedCount = (review.likeCount - 1).coerceAtLeast(0)
            LikeResponse(liked = false, likeCount = updatedCount)
        } else {
            reviewLikeRepository.save(ReviewLike(reviewId = reviewId, userId = googleId))
            incrementLikeCount(reviewId, 1)
            LikeResponse(liked = true, likeCount = review.likeCount + 1)
        }
    }

    fun hasUserLiked(
        reviewId: String,
        googleId: String,
    ): Boolean = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, googleId)

    fun getLikedReviewIds(
        reviewIds: List<String>,
        googleId: String,
    ): Set<String> =
        reviewLikeRepository
            .findByReviewIdInAndUserId(reviewIds, googleId)
            .map { it.reviewId }
            .toSet()

    private fun incrementLikeCount(
        reviewId: String,
        delta: Int,
    ) {
        val query = Query.query(Criteria.where("_id").`is`(reviewId))
        val update = Update().inc("likeCount", delta)
        mongoTemplate.updateFirst(query, update, Review::class.java)
    }
}

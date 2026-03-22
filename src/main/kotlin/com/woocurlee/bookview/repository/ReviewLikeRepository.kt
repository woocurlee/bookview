package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.ReviewLike
import org.springframework.data.mongodb.repository.MongoRepository

interface ReviewLikeRepository : MongoRepository<ReviewLike, String> {
    fun findByReviewIdAndUserId(
        reviewId: String,
        userId: String,
    ): ReviewLike?

    fun existsByReviewIdAndUserId(
        reviewId: String,
        userId: String,
    ): Boolean

    fun deleteByReviewIdAndUserId(
        reviewId: String,
        userId: String,
    )

    fun findByReviewIdInAndUserId(
        reviewIds: List<String>,
        userId: String,
    ): List<ReviewLike>
}

package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Review
import org.springframework.data.mongodb.repository.MongoRepository

interface ReviewRepository : MongoRepository<Review, String> {
    fun findByUserId(userId: String): List<Review>

    fun findByReviewNo(reviewNo: Long): Review?
}

package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.Status
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ReviewRepository : MongoRepository<Review, String> {
    fun findByUserIdAndStatus(
        userId: String,
        status: Status,
    ): List<Review>

    fun findByReviewNoAndStatus(
        reviewNo: Long,
        status: Status,
    ): Review?

    fun findByStatus(
        status: Status,
        pageable: Pageable,
    ): Page<Review>
}

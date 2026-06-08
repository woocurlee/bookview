package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.dto.ReviewSitemapProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

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

    fun countByStatus(status: Status): Long

    @Query(value = "{ 'status': ?0 }", fields = "{ 'reviewNo': 1, 'updatedAt': 1, '_id': 0 }")
    fun findSitemapDataByStatus(
        status: Status,
        pageable: Pageable,
    ): Page<ReviewSitemapProjection>
}

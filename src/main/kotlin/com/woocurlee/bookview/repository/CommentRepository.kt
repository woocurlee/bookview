package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Comment
import com.woocurlee.bookview.domain.Status
import org.springframework.data.mongodb.repository.MongoRepository

interface CommentRepository : MongoRepository<Comment, String> {
    fun findByReviewIdAndStatus(
        reviewId: String,
        status: Status,
    ): List<Comment>

    fun findByParentIdAndStatus(
        parentId: String,
        status: Status,
    ): List<Comment>

    fun findByUserIdAndStatus(
        userId: String,
        status: Status,
    ): List<Comment>

    fun countByReviewIdAndStatus(
        reviewId: String,
        status: Status,
    ): Long
}

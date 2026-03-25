package com.woocurlee.bookview.dto

import com.woocurlee.bookview.domain.Status
import java.time.LocalDateTime

data class CommentResponse(
    val id: String,
    val reviewId: String,
    val userId: String,
    val userNickname: String,
    val content: String,
    val parentId: String?,
    val status: Status,
    val createdAt: LocalDateTime,
)

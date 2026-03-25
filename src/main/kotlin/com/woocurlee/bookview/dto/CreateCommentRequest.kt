package com.woocurlee.bookview.dto

data class CreateCommentRequest(
    val reviewId: String,
    val content: String,
    val parentId: String? = null,
)

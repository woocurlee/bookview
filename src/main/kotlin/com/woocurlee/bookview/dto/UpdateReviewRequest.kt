package com.woocurlee.bookview.dto

data class UpdateReviewRequest(
    val title: String,
    val content: String,
    val rating: Int,
)

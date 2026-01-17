package com.woocurlee.bookview.dto

data class CreateReviewRequest(
    val title: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookIsbn: String,
    val bookThumbnail: String?,
    val rating: Int,
    val quote: String,
    val content: String,
)

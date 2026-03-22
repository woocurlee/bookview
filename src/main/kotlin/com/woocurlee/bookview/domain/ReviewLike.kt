package com.woocurlee.bookview.domain

import com.woocurlee.bookview.common.MongoCollections
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = MongoCollections.REVIEW_LIKES)
@CompoundIndex(name = "review_user_unique", def = "{'reviewId': 1, 'userId': 1}", unique = true)
data class ReviewLike(
    @Id
    val id: String? = null,
    val reviewId: String,
    val userId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

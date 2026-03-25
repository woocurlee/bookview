package com.woocurlee.bookview.domain

import com.woocurlee.bookview.common.MongoCollections
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = MongoCollections.COMMENTS)
data class Comment(
    @Id val id: String? = null,
    val commentNo: Long? = null,
    val reviewId: String,
    val userId: String,
    val content: String,
    val parentId: String? = null,
    val status: Status = Status.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

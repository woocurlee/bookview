package com.woocurlee.bookview.domain

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User(
    @Id
    val id: String? = null,
    val userNo: Long? = null,
    val googleId: String,
    val nickname: String,
    val profileImageUrl: String? = null,
    val email: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastLoginAt: LocalDateTime = LocalDateTime.now(),
)

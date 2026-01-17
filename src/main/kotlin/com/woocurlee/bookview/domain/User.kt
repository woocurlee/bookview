package com.woocurlee.bookview.domain

import com.woocurlee.bookview.common.MongoCollections
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = MongoCollections.USERS)
data class User(
    @Id
    val id: String? = null,
    val userNo: Long? = null,
    val googleId: String,
    val nickname: String,
    val profileImageUrl: String? = null,
    val email: String? = null,
    val status: Status = Status.ACTIVE, // 유저 상태
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastLoginAt: LocalDateTime = LocalDateTime.now(),
)

package com.woocurlee.bookview.dto

import java.time.LocalDateTime

interface ReviewSitemapProjection {
    val reviewNo: Long?
    val updatedAt: LocalDateTime
}

interface UserSitemapProjection {
    val nickname: String
    val lastLoginAt: LocalDateTime
}

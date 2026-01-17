package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
    fun findByGoogleIdAndStatus(
        googleId: String,
        status: Status,
    ): User?

    fun existsByNicknameAndStatus(
        nickname: String,
        status: Status,
    ): Boolean
}

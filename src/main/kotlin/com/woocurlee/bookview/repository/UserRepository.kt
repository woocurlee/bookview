package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    fun findByNicknameAndStatus(
        nickname: String,
        status: Status,
    ): User?

    fun findAllByIsNicknameSetAndStatus(
        isNicknameSet: Boolean,
        status: Status,
        pageable: Pageable,
    ): Page<User>

    fun countByIsNicknameSetAndStatus(
        isNicknameSet: Boolean,
        status: Status,
    ): Long
}

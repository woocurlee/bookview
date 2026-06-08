package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.User
import com.woocurlee.bookview.dto.UserSitemapProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

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

    fun countByIsNicknameSetAndStatus(
        isNicknameSet: Boolean,
        status: Status,
    ): Long

    @Query(value = "{ 'isNicknameSet': ?0, 'status': ?1 }", fields = "{ 'nickname': 1, 'lastLoginAt': 1, '_id': 0 }")
    fun findSitemapDataByIsNicknameSetAndStatus(
        isNicknameSet: Boolean,
        status: Status,
        pageable: Pageable,
    ): Page<UserSitemapProjection>
}

package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.User
import com.woocurlee.bookview.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun findByGoogleId(googleId: String): User? = userRepository.findByGoogleIdAndStatus(googleId, Status.ACTIVE)

    fun updateNickname(
        googleId: String,
        nickname: String,
    ): User? {
        val user = findByGoogleId(googleId) ?: return null
        val updated = user.copy(nickname = nickname)
        return userRepository.save(updated)
    }

    fun deleteUser(googleId: String) {
        val user = findByGoogleId(googleId) ?: return
        val deleted = user.copy(status = Status.DELETED)
        userRepository.save(deleted)
    }
}

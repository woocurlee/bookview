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
        // 닉네임 길이 검증 (1~30자)
        if (nickname.length !in 1..30) {
            throw IllegalArgumentException("닉네임은 1~30자 사이로 입력해주세요.")
        }

        // 영어 소문자, 숫자, 밑줄, 마침표만 허용
        if (!nickname.matches(Regex("^[a-z0-9_.]+$"))) {
            throw IllegalArgumentException("닉네임은 영어 소문자, 숫자, 밑줄(_), 마침표(.)만 사용할 수 있습니다.")
        }

        // 마침표로 시작하거나 끝나는지 검증
        if (nickname.startsWith('.') || nickname.endsWith('.')) {
            throw IllegalArgumentException("닉네임은 마침표(.)로 시작하거나 끝날 수 없습니다.")
        }

        // 연속된 마침표 검증
        if (nickname.contains("..")) {
            throw IllegalArgumentException("마침표(..)를 연속으로 사용할 수 없습니다.")
        }

        val user = findByGoogleId(googleId) ?: return null

        // 닉네임 중복 체크 (본인 제외)
        if (user.nickname != nickname && userRepository.existsByNicknameAndStatus(nickname, Status.ACTIVE)) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
        }

        val updated = user.copy(nickname = nickname, isNicknameSet = true)
        return userRepository.save(updated)
    }

    fun deleteUser(googleId: String) {
        val user = findByGoogleId(googleId) ?: return
        val deleted = user.copy(status = Status.DELETED)
        userRepository.save(deleted)
    }
}

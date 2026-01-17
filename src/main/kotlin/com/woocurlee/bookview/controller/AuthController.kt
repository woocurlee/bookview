package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.toResponse
import com.woocurlee.bookview.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
) {
    @GetMapping("/success")
    fun loginSuccess(
        @AuthenticationPrincipal oauth2User: OAuth2User,
    ): ResponseEntity<Map<String, Any?>> {
        val googleId = oauth2User.attributes["sub"].toString()
        val user = googleId.let { userRepository.findByGoogleIdAndStatus(it, Status.ACTIVE) }

        return ResponseEntity.ok(
            mapOf(
                "message" to "로그인 성공",
                "user" to user?.toResponse(),
            ),
        )
    }

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal oauth2User: OAuth2User?,
    ): ResponseEntity<Any> {
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(mapOf("message" to "인증되지 않은 사용자"))
        }

        val googleId = oauth2User.attributes["sub"].toString()
        val user =
            googleId.let { userRepository.findByGoogleIdAndStatus(it, Status.ACTIVE) }
                ?: return ResponseEntity.status(404).body(mapOf("message" to "사용자를 찾을 수 없습니다"))

        return ResponseEntity.ok(user.toResponse())
    }
}

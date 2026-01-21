package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.toResponse
import com.woocurlee.bookview.dto.UpdateProfileRequest
import com.woocurlee.bookview.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @PutMapping("/profile")
    fun updateProfile(
        @RequestBody request: UpdateProfileRequest,
        @AuthenticationPrincipal principal: Any,
    ): ResponseEntity<Any> {
        val attributes = principal as Map<*, *>
        val googleId = attributes["sub"].toString()

        try {
            val updatedUser =
                userService.updateNickname(
                    googleId,
                    request.nickname,
                    request.agreedToTerms,
                    request.termsVersion,
                ) ?: return ResponseEntity.notFound().build()

            return ResponseEntity.ok(updatedUser.toResponse())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }
}

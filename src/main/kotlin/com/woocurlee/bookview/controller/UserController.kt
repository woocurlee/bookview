package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.toResponse
import com.woocurlee.bookview.dto.UpdateProfileRequest
import com.woocurlee.bookview.dto.UserResponse
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
    ): ResponseEntity<UserResponse> {
        val attributes = principal as Map<*, *>
        val googleId = attributes["sub"].toString()

        val updatedUser =
            userService.updateNickname(googleId, request.nickname)
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(updatedUser.toResponse())
    }
}

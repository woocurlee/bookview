package com.woocurlee.bookview.controller

import com.woocurlee.bookview.dto.LikeResponse
import com.woocurlee.bookview.service.ReviewLikeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reviews")
class ReviewLikeController(
    private val reviewLikeService: ReviewLikeService,
) {
    @PostMapping("/{reviewNo}/like")
    fun toggleLike(
        @PathVariable reviewNo: Long,
        @AuthenticationPrincipal principal: Any,
    ): ResponseEntity<Any> {
        val attributes = principal as Map<*, *>
        val googleId = attributes["sub"].toString()

        return try {
            val result: LikeResponse = reviewLikeService.toggleLike(reviewNo, googleId)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }
}

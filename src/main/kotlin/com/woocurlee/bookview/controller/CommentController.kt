package com.woocurlee.bookview.controller

import com.woocurlee.bookview.dto.CommentResponse
import com.woocurlee.bookview.dto.CreateCommentRequest
import com.woocurlee.bookview.service.CommentService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService,
) {
    @PostMapping
    fun createComment(
        @RequestBody request: CreateCommentRequest,
        @AuthenticationPrincipal principal: Any,
    ): ResponseEntity<Any> {
        val googleId = (principal as Map<*, *>)["sub"].toString()
        return try {
            val comment = commentService.createComment(request, googleId)
            ResponseEntity.ok(comment)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @GetMapping
    fun getComments(
        @RequestParam reviewId: String,
    ): ResponseEntity<List<CommentResponse>> {
        val comments = commentService.getCommentsByReviewId(reviewId)
        return ResponseEntity.ok(comments)
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable commentId: String,
        @AuthenticationPrincipal principal: Any,
    ): ResponseEntity<Any> {
        val googleId = (principal as Map<*, *>)["sub"].toString()
        return try {
            commentService.deleteComment(commentId, googleId)
            ResponseEntity.ok(mapOf("message" to "댓글이 삭제되었습니다."))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }
}

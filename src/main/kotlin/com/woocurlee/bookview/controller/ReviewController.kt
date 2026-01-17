package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.dto.CreateReviewRequest
import com.woocurlee.bookview.dto.UpdateReviewRequest
import com.woocurlee.bookview.service.ReviewService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reviews")
class ReviewController(
    private val reviewService: ReviewService,
) {
    @GetMapping
    fun getAllReviews(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Map<String, Any>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val reviewsPage = reviewService.getReviews(pageable)

        return ResponseEntity.ok(
            mapOf(
                "reviews" to reviewsPage.content,
                "hasMore" to reviewsPage.hasNext(),
            ),
        )
    }

    @PostMapping
    fun createReview(
        @RequestBody request: CreateReviewRequest,
        @AuthenticationPrincipal principal: Any,
    ): ResponseEntity<Review> {
        val attributes = principal as Map<*, *>
        val googleId = attributes["sub"].toString()
        val review =
            Review(
                userId = googleId,
                title = request.title,
                bookTitle = request.bookTitle,
                bookAuthor = request.bookAuthor,
                bookIsbn = request.bookIsbn,
                bookThumbnail = request.bookThumbnail,
                rating = request.rating,
                quote = request.quote,
                content = request.content,
            )
        return ResponseEntity.ok(reviewService.createReview(review))
    }

    @PutMapping("/{id}")
    fun updateReview(
        @PathVariable id: String,
        @RequestBody request: UpdateReviewRequest,
    ): ResponseEntity<Review> {
        val updated =
            reviewService.updateReview(id, request.title, request.content, request.rating)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deleteReview(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        reviewService.deleteReview(id)
        return ResponseEntity.noContent().build()
    }
}

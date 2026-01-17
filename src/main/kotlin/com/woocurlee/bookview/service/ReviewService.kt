package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.repository.ReviewRepository
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val sequenceService: SequenceService,
) {
    fun getReviewsByUserId(userId: String): List<Review> = reviewRepository.findByUserId(userId)

    fun createReview(review: Review): Review {
        val reviewNo = sequenceService.getNextSequence("review_seq")
        val reviewWithNo = review.copy(reviewNo = reviewNo)
        return reviewRepository.save(reviewWithNo)
    }

    fun getAllReviews(): List<Review> = reviewRepository.findAll()

    fun getReviews(pageable: Pageable): Page<Review> = reviewRepository.findAll(pageable)

    fun updateReview(
        id: String,
        title: String,
        content: String,
        rating: Int,
    ): Review? {
        val review = reviewRepository.findById(id).orElse(null) ?: return null
        val updated =
            review.copy(
                title = title,
                content = content,
                rating = rating,
                updatedAt = LocalDateTime.now(),
            )
        return reviewRepository.save(updated)
    }

    fun deleteReview(id: String) {
        reviewRepository.deleteById(id)
    }

    fun getReviewById(id: String): Review? = reviewRepository.findById(id).orElse(null)

    fun getReviewByReviewNo(reviewNo: Long): Review? = reviewRepository.findByReviewNo(reviewNo)
}

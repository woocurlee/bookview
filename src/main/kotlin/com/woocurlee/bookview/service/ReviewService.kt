package com.woocurlee.bookview.service

import com.woocurlee.bookview.common.SequenceNames
import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.Status
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
    fun getReviewsByUserId(userId: String): List<Review> = reviewRepository.findByUserIdAndStatus(userId, Status.ACTIVE)

    fun createReview(review: Review): Review {
        val reviewNo = sequenceService.getNextSequence(SequenceNames.REVIEW_SEQ)
        val reviewWithNo = review.copy(reviewNo = reviewNo)
        return reviewRepository.save(reviewWithNo)
    }

    fun getReviews(pageable: Pageable): Page<Review> = reviewRepository.findByStatus(Status.ACTIVE, pageable)

    fun updateReview(
        id: String,
        title: String,
        content: String,
        rating: Int,
    ): Review? {
        val review = reviewRepository.findById(id).orElse(null) ?: return null
        if (review.status == Status.DELETED) return null

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
        val review = reviewRepository.findById(id).orElse(null) ?: return
        val deleted =
            review.copy(
                status = Status.DELETED,
                updatedAt = LocalDateTime.now(),
            )
        reviewRepository.save(deleted)
    }

    fun getReviewByReviewNo(reviewNo: Long): Review? = reviewRepository.findByReviewNoAndStatus(reviewNo, Status.ACTIVE)
}

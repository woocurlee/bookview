package com.woocurlee.bookview.service

import com.woocurlee.bookview.common.SequenceNames
import com.woocurlee.bookview.domain.BlockAction
import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.TargetType
import com.woocurlee.bookview.dto.ReviewSitemapProjection
import com.woocurlee.bookview.repository.BlockLogRepository
import com.woocurlee.bookview.repository.ReviewRepository
import com.woocurlee.bookview.util.HtmlSanitizer
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

data class BlockedReview(
    val review: Review,
    val blockReason: String?,
)

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val sequenceService: SequenceService,
    private val blockLogRepository: BlockLogRepository,
) {
    fun getReviewsByUserId(userId: String): List<Review> = reviewRepository.findByUserIdAndStatus(userId, Status.ACTIVE)

    /** 본인 마이페이지용: ACTIVE + BLOCK 리뷰 모두 반환 */
    fun getReviewsByUserIdIncludingBlocked(userId: String): List<Review> =
        reviewRepository.findByUserIdAndStatusIn(userId, listOf(Status.ACTIVE, Status.BLOCK))

    /** 리뷰 상세용: ACTIVE + BLOCK 조회 (DELETED 제외). 권한 체크는 컨트롤러에서 한다. */
    fun getReviewByReviewNoIncludingBlocked(reviewNo: Long): BlockedReview? {
        val review =
            reviewRepository.findByReviewNoAndStatusIn(reviewNo, listOf(Status.ACTIVE, Status.BLOCK))
                ?: return null
        val blockReason =
            if (review.status == Status.BLOCK && review.id != null) {
                blockLogRepository
                    .findFirstByTargetTypeAndTargetIdAndActionOrderByCreatedAtDesc(
                        TargetType.REVIEW,
                        review.id,
                        BlockAction.BLOCK,
                    )?.reason
            } else {
                null
            }
        return BlockedReview(review, blockReason)
    }

    fun createReview(review: Review): Review {
        val reviewNo = sequenceService.getNextSequence(SequenceNames.REVIEW_SEQ)

        // XSS 방지: HTML 새니타이즈
        val sanitizedReview =
            review.copy(
                reviewNo = reviewNo,
                title = HtmlSanitizer.toPlainText(review.title),
                content = HtmlSanitizer.sanitize(review.content),
                quote = HtmlSanitizer.toPlainText(review.quote),
            )

        return reviewRepository.save(sanitizedReview)
    }

    fun getReviews(pageable: Pageable): Page<Review> = reviewRepository.findByStatus(Status.ACTIVE, pageable)

    fun updateReview(
        id: String,
        title: String,
        content: String,
        rating: Int,
        quote: String,
    ): Review? {
        val review = reviewRepository.findById(id).orElse(null) ?: return null
        if (review.status == Status.DELETED) return null

        // XSS 방지: HTML 새니타이즈
        val updated =
            review.copy(
                title = HtmlSanitizer.toPlainText(title),
                content = HtmlSanitizer.sanitize(content),
                rating = rating,
                quote = HtmlSanitizer.toPlainText(quote),
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

    fun getReviewByIdInternal(id: String): Review? {
        val review = reviewRepository.findById(id).orElse(null)
        return if (review?.status == Status.ACTIVE) review else null
    }

    fun getActiveReviewsForSitemap(
        page: Int,
        size: Int,
    ): Page<ReviewSitemapProjection> =
        reviewRepository.findSitemapDataByStatus(
            Status.ACTIVE,
            PageRequest.of(page, size),
        )

    fun countActiveReviews(): Long = reviewRepository.countByStatus(Status.ACTIVE)
}

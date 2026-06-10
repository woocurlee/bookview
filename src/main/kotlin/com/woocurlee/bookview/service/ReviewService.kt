package com.woocurlee.bookview.service

import com.woocurlee.bookview.common.SequenceNames
import com.woocurlee.bookview.domain.BlockAction
import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.TargetType
import com.woocurlee.bookview.domain.User
import com.woocurlee.bookview.dto.ReviewSitemapProjection
import com.woocurlee.bookview.repository.BlockLogRepository
import com.woocurlee.bookview.repository.ReviewRepository
import com.woocurlee.bookview.util.HtmlSanitizer
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

data class ReviewDetail(
    val review: Review,
    val author: User?,
    val isBlocked: Boolean,
    val blockReason: String?,
    val isOwner: Boolean,
) {
    /** 차단된 글은 본인만 접근 가능 */
    val isAccessible: Boolean get() = !isBlocked || isOwner
}

data class ReviewStats(
    val avgRating: String,
    val totalLikes: Long,
)

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val sequenceService: SequenceService,
    private val blockLogRepository: BlockLogRepository,
    private val userService: UserService,
) {
    fun getReviewsByUserId(userId: String): List<Review> = reviewRepository.findByUserIdAndStatus(userId, Status.ACTIVE)

    /** 본인 마이페이지용: ACTIVE + BLOCK 리뷰 모두 반환 */
    fun getReviewsByUserIdIncludingBlocked(userId: String): List<Review> =
        reviewRepository.findByUserIdAndStatusIn(userId, listOf(Status.ACTIVE, Status.BLOCK))

    /**
     * 리뷰 상세 조회. ACTIVE/BLOCK 모두 반환하며 소유권·차단 정보를 함께 제공한다.
     * DELETED는 null 반환.
     */
    fun getReviewDetail(
        reviewNo: Long,
        currentGoogleId: String?,
    ): ReviewDetail? {
        val review =
            reviewRepository.findByReviewNoAndStatusIn(reviewNo, listOf(Status.ACTIVE, Status.BLOCK))
                ?: return null
        val author = userService.findByGoogleId(review.userId)
        val isOwner = currentGoogleId != null && currentGoogleId == review.userId
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
        return ReviewDetail(
            review = review,
            author = author,
            isBlocked = review.status == Status.BLOCK,
            blockReason = blockReason,
            isOwner = isOwner,
        )
    }

    /** 리뷰 목록의 평균 별점·총 좋아요 수를 계산한다. */
    fun calculateStats(reviews: List<Review>): ReviewStats {
        val avg = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
        return ReviewStats(
            avgRating = String.format("%.1f", avg),
            totalLikes = reviews.sumOf { it.likeCount },
        )
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

package com.woocurlee.bookview.controller

import com.woocurlee.bookview.repository.UserRepository
import com.woocurlee.bookview.service.ReviewService
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ViewController(
    private val userRepository: UserRepository,
    private val reviewService: ReviewService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/")
    fun index(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        // 최근 리뷰 10개 추가 (페이징)
        val pageable =
            org.springframework.data.domain.PageRequest.of(
                0,
                10,
                org.springframework.data.domain.Sort
                    .by("createdAt")
                    .descending(),
            )
        val reviewsPage = reviewService.getReviews(pageable)
        model.addAttribute("reviews", reviewsPage.content)
        model.addAttribute("hasMoreReviews", reviewsPage.hasNext())

        if (principal != null) {
            val attributes = principal as? Map<*, *>
            val googleId = attributes?.get("sub")?.toString()
            if (googleId != null) {
                val user = userRepository.findByGoogleId(googleId)
                model.addAttribute("user", user)
            }
        }

        return "index"
    }

    @GetMapping("/write-review")
    fun writeReview(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal != null) {
            val attributes = principal as? Map<*, *>
            val googleId = attributes?.get("sub")?.toString()
            if (googleId != null) {
                val user = userRepository.findByGoogleId(googleId)
                model.addAttribute("user", user)
            }
        }
        return "write-review"
    }

    @GetMapping("/my-page")
    fun myPage(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null) {
            return "redirect:/oauth2/authorization/google"
        }

        val attributes = principal as? Map<*, *>
        val googleId = attributes?.get("sub")?.toString()
        if (googleId != null) {
            val user = userRepository.findByGoogleId(googleId)
            model.addAttribute("user", user)

            // 내가 쓴 리뷰 가져오기 (최신순 정렬)
            val myReviews = reviewService.getReviewsByUserId(googleId).sortedByDescending { it.createdAt }
            model.addAttribute("myReviews", myReviews)

            // 평균 별점 계산
            val avgRating = if (myReviews.isEmpty()) 0.0 else myReviews.map { it.rating }.average()
            model.addAttribute("avgRating", String.format("%.1f", avgRating))
        }

        return "my-page"
    }

    @GetMapping("/review/{id}")
    fun reviewDetail(
        @org.springframework.web.bind.annotation.PathVariable id: String,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val review = reviewService.getReviewById(id) ?: return "redirect:/"
        model.addAttribute("review", review)

        // 작성자 정보
        val author = userRepository.findByGoogleId(review.userId)
        model.addAttribute("author", author)

        // 현재 사용자 정보
        if (principal != null) {
            val attributes = principal as? Map<*, *>
            val googleId = attributes?.get("sub")?.toString()
            if (googleId != null) {
                val user = userRepository.findByGoogleId(googleId)
                model.addAttribute("user", user)
            }
        }

        return "review-detail"
    }
}

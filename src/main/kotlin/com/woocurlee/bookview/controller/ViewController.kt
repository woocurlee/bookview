package com.woocurlee.bookview.controller

import com.woocurlee.bookview.service.ReviewLikeService
import com.woocurlee.bookview.service.ReviewService
import com.woocurlee.bookview.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ViewController(
    private val userService: UserService,
    private val reviewService: ReviewService,
    private val reviewLikeService: ReviewLikeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/")
    fun index(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        // 로그인하지 않은 유저는 랜딩 페이지로
        if (principal == null || principal.toString() == "anonymousUser") {
            return "landing"
        }

        // 닉네임 미설정 체크
        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) {
            return "redirect:/setup-nickname"
        }

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

        // 좋아요 여부
        if (user != null) {
            val reviewIds = reviewsPage.content.mapNotNull { it.id }
            val likedReviewIds = reviewLikeService.getLikedReviewIds(reviewIds, user.googleId)
            model.addAttribute("likedReviewIds", likedReviewIds)
        } else {
            model.addAttribute("likedReviewIds", emptySet<String>())
        }

        return "index"
    }

    @GetMapping("/setup-nickname")
    fun setupNickname(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null) {
            return "redirect:/oauth2/authorization/google"
        }

        // 이미 닉네임이 설정된 경우 홈으로 리다이렉트
        val user = addUserToModel(principal, userService, model)
        if (user != null && user.isNicknameSet) {
            return "redirect:/"
        }

        return "setup-nickname"
    }

    @GetMapping("/write-review")
    fun writeReview(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) {
            return "redirect:/setup-nickname"
        }
        return "write-review"
    }

    @GetMapping("/my-page")
    fun myPage(
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null) {
            return "redirect:/oauth2/authorization/google"
        }
        val attributes = principal as? Map<*, *>
        val googleId = attributes?.get("sub")?.toString() ?: return "redirect:/oauth2/authorization/google"
        val user = userService.findByGoogleId(googleId) ?: return "redirect:/oauth2/authorization/google"
        if (!user.isNicknameSet) return "redirect:/setup-nickname"
        return "redirect:/u/${user.nickname}"
    }

    @GetMapping("/u/{nickname}")
    fun userPage(
        @org.springframework.web.bind.annotation.PathVariable nickname: String,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val profileUser = userService.findByNickname(nickname) ?: return "error/404"

        // 현재 로그인 유저 확인
        val currentUser = addUserToModel(principal, userService, model)
        if (currentUser != null && !currentUser.isNicknameSet) {
            return "redirect:/setup-nickname"
        }
        val isOwner = currentUser?.googleId == profileUser.googleId

        model.addAttribute("profileUser", profileUser)
        model.addAttribute("isOwner", isOwner)

        val reviews = reviewService.getReviewsByUserId(profileUser.googleId).sortedByDescending { it.createdAt }
        model.addAttribute("reviews", reviews)

        val avgRating = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
        model.addAttribute("avgRating", String.format("%.1f", avgRating))

        // 좋아요 여부
        if (currentUser != null) {
            val reviewIds = reviews.mapNotNull { it.id }
            val likedReviewIds = reviewLikeService.getLikedReviewIds(reviewIds, currentUser.googleId)
            model.addAttribute("likedReviewIds", likedReviewIds)
        } else {
            model.addAttribute("likedReviewIds", emptySet<String>())
        }

        return "user-page"
    }

    @GetMapping("/r/{reviewNo}")
    fun reviewDetail(
        @org.springframework.web.bind.annotation.PathVariable reviewNo: Long,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) {
            return "redirect:/setup-nickname"
        }

        val review = reviewService.getReviewByReviewNo(reviewNo) ?: return "redirect:/"
        model.addAttribute("review", review)

        // 작성자 정보
        val author = userService.findByGoogleId(review.userId)
        model.addAttribute("author", author)

        // 좋아요 여부
        val hasLiked =
            user != null &&
                review.id != null &&
                reviewLikeService.hasUserLiked(review.id, user.googleId)
        model.addAttribute("hasLiked", hasLiked)

        return "review-detail"
    }

    @GetMapping("/r/{reviewNo}/edit")
    fun editReview(
        @org.springframework.web.bind.annotation.PathVariable reviewNo: Long,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null || principal.toString() == "anonymousUser") {
            return "redirect:/oauth2/authorization/google"
        }

        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) {
            return "redirect:/setup-nickname"
        }

        val review = reviewService.getReviewByReviewNo(reviewNo) ?: return "redirect:/"

        // 본인 리뷰가 아니면 상세 페이지로
        val attributes = principal as? Map<*, *>
        val googleId = attributes?.get("sub")?.toString()
        if (googleId != review.userId) {
            return "redirect:/r/$reviewNo"
        }

        model.addAttribute("review", review)
        return "edit-review"
    }

    @GetMapping("/privacy-policy")
    fun privacyPolicy(): String = "redirect:/privacy-policy.html"

    @GetMapping("/terms-of-service")
    fun termsOfService(): String = "redirect:/terms-of-service.html"
}

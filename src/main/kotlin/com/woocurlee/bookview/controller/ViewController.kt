package com.woocurlee.bookview.controller

import com.woocurlee.bookview.dto.CommentResponse
import com.woocurlee.bookview.service.CommentService
import com.woocurlee.bookview.service.ReviewDetail
import com.woocurlee.bookview.service.ReviewLikeService
import com.woocurlee.bookview.service.ReviewService
import com.woocurlee.bookview.service.UserPageService
import com.woocurlee.bookview.service.UserService
import com.woocurlee.bookview.util.HtmlSanitizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import tools.jackson.databind.ObjectMapper

@Controller
class ViewController(
    private val userService: UserService,
    private val reviewService: ReviewService,
    private val reviewLikeService: ReviewLikeService,
    private val commentService: CommentService,
    private val userPageService: UserPageService,
    private val objectMapper: ObjectMapper,
    @Value("\${app.base-url}") private val baseUrl: String,
) {
    @GetMapping("/")
    fun index(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null || principal.toString() == "anonymousUser") {
            return "landing"
        }

        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) {
            return "redirect:/setup-nickname"
        }

        val pageable =
            PageRequest.of(
                0,
                10,
                Sort.by("createdAt").descending(),
            )
        val reviewsPage = reviewService.getReviews(pageable)
        model.addAttribute("reviews", reviewsPage.content)
        model.addAttribute("hasMoreReviews", reviewsPage.hasNext())
        model.addAttribute("canonicalUrl", baseUrl)

        val likedReviewIds =
            reviewLikeService.getLikedReviewIdsOrEmpty(
                reviewsPage.content.mapNotNull {
                    it.id
                },
                user?.googleId,
            )
        model.addAttribute("likedReviewIds", likedReviewIds)

        return "index"
    }

    @GetMapping("/setup-nickname")
    fun setupNickname(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null) return "redirect:/oauth2/authorization/google"
        val user = addUserToModel(principal, userService, model)
        if (user != null && user.isNicknameSet) return "redirect:/"
        return "setup-nickname"
    }

    @GetMapping("/write-review")
    fun writeReview(
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) return "redirect:/setup-nickname"
        return "write-review"
    }

    @GetMapping("/my-page")
    fun myPage(
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null) return "redirect:/oauth2/authorization/google"
        val attributes = principal as? Map<*, *>
        val googleId = attributes?.get("sub")?.toString() ?: return "redirect:/oauth2/authorization/google"
        val user = userService.findByGoogleId(googleId) ?: return "redirect:/oauth2/authorization/google"
        if (!user.isNicknameSet) return "redirect:/setup-nickname"
        return "redirect:/u/${user.nickname}"
    }

    @GetMapping("/u/{nickname}")
    fun userPage(
        @PathVariable nickname: String,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val currentUser = addUserToModel(principal, userService, model)
        if (currentUser != null && !currentUser.isNicknameSet) return "redirect:/setup-nickname"

        val data =
            userPageService.getUserPageData(nickname, currentUser?.googleId) ?: run {
                model.addAttribute("status", 404)
                model.addAttribute("message", "존재하지 않는 사용자입니다")
                model.addAttribute("detail", "@$nickname 사용자를 찾을 수 없습니다.")
                return "error"
            }

        model.addAttribute("profileUser", data.profileUser)
        model.addAttribute("isOwner", data.isOwner)
        model.addAttribute("reviews", data.reviews)
        model.addAttribute("avgRating", data.stats.avgRating)
        model.addAttribute("totalLikes", data.stats.totalLikes)
        model.addAttribute("likedReviewIds", data.likedReviewIds)
        model.addAttribute(
            "metaDescription",
            "${data.profileUser.nickname}의 BookView 프로필 - ${data.reviews.size}개의 리뷰, 평균 별점 ${data.stats.avgRating}",
        )
        model.addAttribute("canonicalUrl", "$baseUrl/u/${data.profileUser.nickname}")
        return "user-page"
    }

    @GetMapping("/r/{reviewNo}")
    fun reviewDetail(
        @PathVariable reviewNo: Long,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) return "redirect:/setup-nickname"

        val detail = reviewService.getReviewDetail(reviewNo, user?.googleId) ?: return "redirect:/"
        if (!detail.isAccessible) {
            model.addAttribute("status", 404)
            model.addAttribute("message", "존재하지 않는 글입니다")
            model.addAttribute("detail", "")
            return "error"
        }

        val commentTree = detail.review.id?.let { commentService.getCommentTree(it) }
        val hasLiked = reviewLikeService.hasUserLikedOrFalse(detail.review.id, user?.googleId)

        model.addAttribute("review", detail.review)
        model.addAttribute("author", detail.author)
        model.addAttribute("isBlocked", detail.isBlocked)
        model.addAttribute("blockReason", detail.blockReason)
        model.addAttribute("hasLiked", hasLiked)
        model.addAttribute("topLevelComments", commentTree?.topLevelComments ?: emptyList<CommentResponse>())
        model.addAttribute("commentRepliesMap", commentTree?.repliesMap ?: emptyMap<String, List<CommentResponse>>())
        model.addAttribute("commentCount", commentTree?.count ?: 0)

        // SEO
        model.addAttribute("seoTitle", "${detail.review.title} - ${detail.review.bookTitle} | BookView")
        model.addAttribute(
            "metaDescription",
            "${detail.review.bookTitle} (${detail.review.bookAuthor}) 리뷰 - ${HtmlSanitizer.toPlainText(
                detail.review.content,
            ).take(150)}",
        )
        model.addAttribute(
            "ogImage",
            detail.review.bookThumbnail?.takeIf { it.isNotEmpty() } ?: "$baseUrl/images/bookview-og.png",
        )
        model.addAttribute("ogType", "article")
        model.addAttribute("canonicalUrl", "$baseUrl/r/${detail.review.reviewNo}")
        model.addAttribute("jsonLd", buildJsonLd(detail))

        return "review-detail"
    }

    @GetMapping("/r/{reviewNo}/edit")
    fun editReview(
        @PathVariable reviewNo: Long,
        model: Model,
        @AuthenticationPrincipal principal: Any?,
    ): String {
        if (principal == null || principal.toString() == "anonymousUser") {
            return "redirect:/oauth2/authorization/google"
        }

        val user = addUserToModel(principal, userService, model)
        if (user != null && !user.isNicknameSet) return "redirect:/setup-nickname"

        val review = reviewService.getReviewByReviewNo(reviewNo) ?: return "redirect:/"

        val attributes = principal as? Map<*, *>
        val googleId = attributes?.get("sub")?.toString()
        if (googleId != review.userId) return "redirect:/r/$reviewNo"

        model.addAttribute("review", review)
        return "write-review"
    }

    @GetMapping("/login-error")
    fun loginError(model: Model): String {
        model.addAttribute("status", 500)
        model.addAttribute("message", "로그인에 실패했습니다")
        model.addAttribute("detail", "잠시 후 다시 시도해주세요.")
        return "error"
    }

    @GetMapping("/privacy-policy")
    fun privacyPolicy(): String = "redirect:/privacy-policy.html"

    @GetMapping("/terms-of-service")
    fun termsOfService(): String = "redirect:/terms-of-service.html"

    private fun buildJsonLd(detail: ReviewDetail): String {
        val jsonLd =
            objectMapper
                .writeValueAsString(
                    mapOf(
                        "@context" to "https://schema.org",
                        "@type" to "Review",
                        "name" to detail.review.title,
                        "reviewRating" to
                            mapOf(
                                "@type" to "Rating",
                                "ratingValue" to detail.review.rating,
                                "bestRating" to 5,
                                "worstRating" to 1,
                            ),
                        "author" to mapOf("@type" to "Person", "name" to (detail.author?.nickname ?: "")),
                        "itemReviewed" to
                            mapOf(
                                "@type" to "Book",
                                "name" to detail.review.bookTitle,
                                "author" to mapOf("@type" to "Person", "name" to detail.review.bookAuthor),
                            ),
                        "datePublished" to
                            detail.review.createdAt
                                .toLocalDate()
                                .toString(),
                    ),
                ).replace("<", "\\u003c")
        return jsonLd
    }
}

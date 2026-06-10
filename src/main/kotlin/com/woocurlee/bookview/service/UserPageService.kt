package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.domain.User
import org.springframework.stereotype.Service

data class UserPageData(
    val profileUser: User,
    val isOwner: Boolean,
    val reviews: List<Review>,
    val stats: ReviewStats,
    val likedReviewIds: Set<String>,
)

@Service
class UserPageService(
    private val userService: UserService,
    private val reviewService: ReviewService,
    private val reviewLikeService: ReviewLikeService,
) {
    /**
     * 유저 페이지에 필요한 데이터를 조회한다.
     * 본인 페이지인 경우 차단된 리뷰도 포함.
     * @return 프로필 유저가 없으면 null
     */
    fun getUserPageData(
        nickname: String,
        currentGoogleId: String?,
    ): UserPageData? {
        val profileUser = userService.findByNickname(nickname) ?: return null
        val isOwner = currentGoogleId != null && currentGoogleId == profileUser.googleId

        val reviews =
            if (isOwner) {
                reviewService.getReviewsByUserIdIncludingBlocked(profileUser.googleId)
            } else {
                reviewService.getReviewsByUserId(profileUser.googleId)
            }

        val stats = reviewService.calculateStats(reviews)
        val likedReviewIds = reviewLikeService.getLikedReviewIdsOrEmpty(reviews.mapNotNull { it.id }, currentGoogleId)

        return UserPageData(
            profileUser = profileUser,
            isOwner = isOwner,
            reviews = reviews,
            stats = stats,
            likedReviewIds = likedReviewIds,
        )
    }
}

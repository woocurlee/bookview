package com.woocurlee.bookview.controller

import com.woocurlee.bookview.dto.ReviewSitemapProjection
import com.woocurlee.bookview.dto.UserSitemapProjection
import com.woocurlee.bookview.service.ReviewService
import com.woocurlee.bookview.service.UserService
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class SitemapControllerTest {
    @Mock lateinit var reviewService: ReviewService

    @Mock lateinit var userService: UserService

    private lateinit var controller: SitemapController

    @BeforeEach
    fun setUp() {
        controller = SitemapController(reviewService, userService, "https://bookview.my")
    }

    @Test
    fun `sitemap index - 리뷰 1500건이면 서브 sitemap 2개 생성`() {
        given(reviewService.countActiveReviews()).willReturn(1500L)
        given(userService.countActiveUsersWithNickname()).willReturn(50L)

        val body = controller.sitemapIndex().body!!

        assertThat(body).contains("sitemap-reviews-0.xml")
        assertThat(body).contains("sitemap-reviews-1.xml")
        assertThat(body).doesNotContain("sitemap-reviews-2.xml")
        assertThat(body).contains("sitemap-users-0.xml")
        assertThat(body).doesNotContain("sitemap-users-1.xml")
    }

    @Test
    fun `sitemap index - 데이터 없어도 서브 sitemap 1개는 생성`() {
        given(reviewService.countActiveReviews()).willReturn(0L)
        given(userService.countActiveUsersWithNickname()).willReturn(0L)

        val body = controller.sitemapIndex().body!!

        assertThat(body).contains("sitemap-reviews-0.xml")
        assertThat(body).contains("sitemap-users-0.xml")
    }

    @Test
    fun `reviews sitemap - URL과 lastmod 포함`() {
        val projection =
            object : ReviewSitemapProjection {
                override val reviewNo = 42L
                override val updatedAt: LocalDateTime = LocalDateTime.of(2024, 6, 1, 0, 0)
            }
        given(reviewService.getActiveReviewsForSitemap(0, 1000)).willReturn(PageImpl(listOf(projection)))

        val body = controller.reviewsSitemap(0).body!!

        assertThat(body).contains("https://bookview.my/r/42")
        assertThat(body).contains("2024-06-01")
    }

    @Test
    fun `reviews sitemap - reviewNo null이면 URL 제외`() {
        val projection =
            object : ReviewSitemapProjection {
                override val reviewNo: Long? = null
                override val updatedAt: LocalDateTime = LocalDateTime.now()
            }
        given(reviewService.getActiveReviewsForSitemap(0, 1000)).willReturn(PageImpl(listOf(projection)))

        val body = controller.reviewsSitemap(0).body!!

        assertThat(body).doesNotContain("<loc>")
    }

    @Test
    fun `reviews sitemap - page 음수이면 400 반환`() {
        assertThat(controller.reviewsSitemap(-1).statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `users sitemap - page 음수이면 400 반환`() {
        assertThat(controller.usersSitemap(-1).statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `users sitemap - URL과 lastmod 포함`() {
        val projection =
            object : UserSitemapProjection {
                override val nickname = "bookworm"
                override val lastLoginAt: LocalDateTime = LocalDateTime.of(2024, 6, 8, 0, 0)
            }
        given(userService.getActiveUsersForSitemap(0, 1000)).willReturn(PageImpl(listOf(projection)))

        val body = controller.usersSitemap(0).body!!

        assertThat(body).contains("https://bookview.my/u/bookworm")
        assertThat(body).contains("2024-06-08")
    }
}

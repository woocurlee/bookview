package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.Review
import com.woocurlee.bookview.service.CommentService
import com.woocurlee.bookview.service.ReviewLikeService
import com.woocurlee.bookview.service.ReviewService
import com.woocurlee.bookview.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.ui.ExtendedModelMap
import tools.jackson.databind.ObjectMapper

@ExtendWith(MockitoExtension::class)
class ViewControllerJsonLdTest {
    @Mock lateinit var userService: UserService

    @Mock lateinit var reviewService: ReviewService

    @Mock lateinit var reviewLikeService: ReviewLikeService

    @Mock lateinit var commentService: CommentService

    private lateinit var controller: ViewController
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        controller =
            ViewController(
                userService,
                reviewService,
                reviewLikeService,
                commentService,
                objectMapper,
                "https://bookview.my",
            )
    }

    @Test
    fun `JSON-LD - 올바른 스키마 구조 생성`() {
        given(reviewService.getReviewByReviewNo(1L)).willReturn(review())

        val model = ExtendedModelMap()
        controller.reviewDetail(1L, model, null)

        val jsonLd = model["jsonLd"] as String
        assertThat(jsonLd).contains("\"@context\":\"https://schema.org\"")
        assertThat(jsonLd).contains("\"@type\":\"Review\"")
        assertThat(jsonLd).contains("\"ratingValue\":5")
        assertThat(jsonLd).contains("\"datePublished\"")
    }

    @Test
    fun `JSON-LD - 탭과 줄바꿈 등 제어 문자 올바르게 이스케이프`() {
        given(reviewService.getReviewByReviewNo(1L)).willReturn(
            review(title = "제목\t탭\n줄바꿈"),
        )

        val model = ExtendedModelMap()
        controller.reviewDetail(1L, model, null)

        val jsonLd = model["jsonLd"] as String
        // 파싱 가능한 유효한 JSON인지 확인
        assertThat(objectMapper.readTree(jsonLd)).isNotNull()
        assertThat(jsonLd).doesNotContain("\t").doesNotContain("\n")
    }

    @Test
    fun `JSON-LD - 따옴표 포함된 제목 이스케이프`() {
        given(reviewService.getReviewByReviewNo(1L)).willReturn(
            review(title = "\"작은따옴표\" 포함"),
        )

        val model = ExtendedModelMap()
        controller.reviewDetail(1L, model, null)

        val jsonLd = model["jsonLd"] as String
        assertThat(objectMapper.readTree(jsonLd)).isNotNull()
        @Suppress("UNCHECKED_CAST")
        val parsed = objectMapper.readValue(jsonLd, Map::class.java) as Map<String, Any>
        assertThat(parsed["name"]).isEqualTo("\"작은따옴표\" 포함")
    }

    private fun review(
        title: String = "리뷰 제목",
        bookTitle: String = "책 제목",
        bookAuthor: String = "저자",
        rating: Int = 5,
    ) = Review(
        id = null,
        reviewNo = 1L,
        userId = "user1",
        title = title,
        bookTitle = bookTitle,
        bookAuthor = bookAuthor,
        bookIsbn = "1234567890",
        bookThumbnail = null,
        rating = rating,
        quote = "",
        content = "<p>내용</p>",
    )
}

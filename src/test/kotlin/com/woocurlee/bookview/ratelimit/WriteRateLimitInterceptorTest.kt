package com.woocurlee.bookview.ratelimit

import com.woocurlee.bookview.controller.CommentController
import com.woocurlee.bookview.controller.ReviewController
import kotlin.test.assertFailsWith
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod
import org.springframework.web.server.ResponseStatusException

class WriteRateLimitInterceptorTest {
    private val limiter = WriteRateLimiter(WriteRateLimitProperties()) { 0L }
    private val interceptor = WriteRateLimitInterceptor(limiter)

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `세 쓰기 핸들러에서 한도 초과 시 429와 대기 시간을 반환한다`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(mapOf("sub" to "google-user"), null, emptyList())
        for ((type, name, limit, seconds) in listOf(
            Endpoint(ReviewController::class.java, "createReview", 5, 600),
            Endpoint(ReviewController::class.java, "updateReview", 10, 60),
            Endpoint(CommentController::class.java, "createComment", 5, 60),
        )) {
            val handler = HandlerMethod(mock(type), type.methods.single { it.name == name })
            repeat(limit) {
                assertThat(interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), handler)).isTrue()
            }
            val response = MockHttpServletResponse()
            assertThat(interceptor.preHandle(MockHttpServletRequest(), response, handler)).isFalse()
            assertThat(response.status).isEqualTo(429)
            assertThat(response.getHeader("Retry-After")).isEqualTo(seconds.toString())
            assertThat(response.contentAsString).contains("${seconds}초 후", "\"retryAfterSeconds\":$seconds")
        }
    }

    @Test
    fun `미인증 작성은 기록 없이 거절하고 다른 핸들러는 통과한다`() {
        val type = CommentController::class.java
        val handler = HandlerMethod(mock(type), type.methods.single { it.name == "createComment" })
        val error =
            assertFailsWith<ResponseStatusException> {
                interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), handler)
            }
        assertThat(error.statusCode.value()).isEqualTo(401)
        assertThat(limiter.trackedKeyCount).isZero()
        assertThat(interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any())).isTrue()
    }

    private data class Endpoint(
        val type: Class<*>,
        val name: String,
        val limit: Int,
        val seconds: Int,
    )
}

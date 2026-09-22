package com.woocurlee.bookview.ratelimit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource

class WriteRateLimitConfigTest {
    @Test
    fun `설정값을 바인딩하고 동일한 제한 컴포넌트를 공유한다`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(
                MapPropertySource(
                    "test",
                    mapOf("app.rate-limit.review-create.limit" to "1", "app.rate-limit.review-create.window" to "2m"),
                ),
            )
            context.register(WriteRateLimitConfig::class.java)
            context.refresh()
            val limiter = context.getBean(WriteRateLimiter::class.java)
            assertThat(context.getBean(WriteRateLimitConfig::class.java).writeRateLimiter()).isSameAs(limiter)
            assertThat(limiter.acquire("user", WriteAction.REVIEW_CREATE)).isZero()
            assertThat(limiter.acquire("user", WriteAction.REVIEW_CREATE)).isBetween(119L, 120L)
        }
    }
}

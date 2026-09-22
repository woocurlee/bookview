package com.woocurlee.bookview.ratelimit

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WriteRateLimiterTest {
    @Test
    fun `작업별 한도와 사용자별 기록을 분리한다`() {
        val limiter = WriteRateLimiter(WriteRateLimitProperties()) { 0L }
        for ((action, limit) in listOf(
            WriteAction.REVIEW_CREATE to 5,
            WriteAction.REVIEW_UPDATE to 10,
            WriteAction.COMMENT_CREATE to 5,
        )) {
            repeat(limit) { assertThat(limiter.acquire("user", action)).isZero() }
            assertThat(limiter.acquire("user", action)).isPositive()
            assertThat(limiter.acquire("other", action)).isZero()
        }
    }

    @Test
    fun `차단 요청은 기록을 늘리거나 대기 시간을 연장하지 않는다`() {
        var now = 0L
        val limiter = WriteRateLimiter(WriteRateLimitProperties()) { now }
        repeat(5) { limiter.acquire("user", WriteAction.REVIEW_CREATE) }
        now = 599_001
        repeat(10_000) { assertThat(limiter.acquire("user", WriteAction.REVIEW_CREATE)).isEqualTo(1) }
        assertThat(limiter.trackedKeyCount).isEqualTo(1)
        now = 600_000
        assertThat(limiter.acquire("user", WriteAction.REVIEW_CREATE)).isZero()
        limiter.cleanup()
        assertThat(limiter.trackedKeyCount).isEqualTo(1)
        now = 1_200_000
        limiter.cleanup()
        assertThat(limiter.trackedKeyCount).isZero()
    }

    @Test
    fun `슬라이딩 윈도우는 만료된 요청 수만큼만 허용한다`() {
        var now = 0L
        val limiter = WriteRateLimiter(WriteRateLimitProperties()) { now }
        limiter.acquire("user", WriteAction.REVIEW_CREATE)
        now = 100_000
        repeat(4) { limiter.acquire("user", WriteAction.REVIEW_CREATE) }
        now = 600_000
        assertThat(limiter.acquire("user", WriteAction.REVIEW_CREATE)).isZero()
        assertThat(limiter.acquire("user", WriteAction.REVIEW_CREATE)).isEqualTo(100)
    }

    @Test
    fun `동시 요청과 정리 중에도 한도를 넘지 않는다`() {
        val limiter = WriteRateLimiter(WriteRateLimitProperties()) { 0L }
        Executors.newFixedThreadPool(16).use { executor ->
            val results =
                executor.invokeAll(
                    (1..200).map {
                        Callable {
                            limiter.cleanup()
                            limiter.acquire("user", WriteAction.COMMENT_CREATE)
                        }
                    },
                )
            assertThat(results.count { it.get() == 0L }).isEqualTo(5)
        }
    }
}

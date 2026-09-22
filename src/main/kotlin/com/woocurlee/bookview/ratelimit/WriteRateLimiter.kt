package com.woocurlee.bookview.ratelimit

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled

@ConfigurationProperties("app.rate-limit")
data class WriteRateLimitProperties(
    val reviewCreate: Policy = Policy(5, Duration.ofMinutes(10)),
    val reviewUpdate: Policy = Policy(10, Duration.ofMinutes(1)),
    val commentCreate: Policy = Policy(5, Duration.ofMinutes(1)),
) {
    data class Policy(
        val limit: Int,
        val window: Duration,
    ) {
        init {
            require(limit > 0 && window.toMillis() > 0) { "제한 횟수와 시간은 양수여야 합니다." }
        }
    }

    fun policy(action: WriteAction): Policy =
        when (action) {
            WriteAction.REVIEW_CREATE -> reviewCreate
            WriteAction.REVIEW_UPDATE -> reviewUpdate
            WriteAction.COMMENT_CREATE -> commentCreate
        }
}

enum class WriteAction { REVIEW_CREATE, REVIEW_UPDATE, COMMENT_CREATE }

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimited(
    val action: WriteAction,
)

// 단일 인스턴스 전용이며 재시작하면 기록이 초기화됩니다.
class WriteRateLimiter(
    private val properties: WriteRateLimitProperties,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private data class Key(
        val userId: String,
        val action: WriteAction,
    )

    private val records = ConcurrentHashMap<Key, ArrayDeque<Long>>()

    internal val trackedKeyCount: Int get() = records.size

    // 0이면 통과, 양수이면 다시 시도할 때까지 남은 초입니다.
    fun acquire(
        userId: String,
        action: WriteAction,
    ): Long {
        val policy = properties.policy(action)
        var retryAfter = 0L
        records.compute(Key(userId, action)) { _, existing ->
            val now = nowMillis()
            val timestamps = existing ?: ArrayDeque()
            expire(timestamps, now, policy.window.toMillis())
            if (timestamps.size < policy.limit) {
                timestamps.addLast(now)
            } else {
                val remaining = policy.window.toMillis() - (now - timestamps.first())
                retryAfter = (remaining + 999) / 1000
            }
            timestamps
        }
        return retryAfter
    }

    @Scheduled(fixedDelay = 60_000)
    fun cleanup() {
        records.keys.forEach { key ->
            // 요청 처리와 같은 키의 compute를 사용해 정리 중 새 기록이 유실되지 않도록 합니다.
            records.computeIfPresent(key) { _, timestamps ->
                expire(timestamps, nowMillis(), properties.policy(key.action).window.toMillis())
                timestamps.takeUnless { it.isEmpty() }
            }
        }
    }

    private fun expire(
        timestamps: ArrayDeque<Long>,
        now: Long,
        window: Long,
    ) {
        while (timestamps.isNotEmpty() && now - timestamps.first() >= window) {
            timestamps.removeFirst()
        }
    }
}

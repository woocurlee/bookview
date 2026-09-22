package com.woocurlee.bookview.ratelimit

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableScheduling
@EnableConfigurationProperties(WriteRateLimitProperties::class)
class WriteRateLimitConfig(
    private val properties: WriteRateLimitProperties,
) : WebMvcConfigurer {
    @Bean
    fun writeRateLimiter(): WriteRateLimiter = WriteRateLimiter(properties)

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(WriteRateLimitInterceptor(writeRateLimiter()))
    }
}

class WriteRateLimitInterceptor(
    private val limiter: WriteRateLimiter,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val action = (handler as? HandlerMethod)?.getMethodAnnotation(RateLimited::class.java)?.action ?: return true
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = (authentication?.principal as? Map<*, *>)?.get("sub") as? String
        if (authentication?.isAuthenticated != true || userId.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")
        }
        val retryAfter = limiter.acquire(userId, action)
        if (retryAfter == 0L) return true
        response.status = 429
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.setHeader("Retry-After", retryAfter.toString())
        response.writer.write(
            """{"message":"요청이 너무 많습니다. ${retryAfter}초 후 다시 시도해주세요.","retryAfterSeconds":$retryAfter}""",
        )
        return false
    }
}

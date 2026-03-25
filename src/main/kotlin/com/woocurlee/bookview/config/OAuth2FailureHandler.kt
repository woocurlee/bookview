package com.woocurlee.bookview.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2FailureHandler : SimpleUrlAuthenticationFailureHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorCode =
            (exception.cause as? OAuth2AuthorizationException)?.error?.errorCode
                ?: (exception as? OAuth2AuthorizationException)?.error?.errorCode

        if (errorCode == "access_denied") {
            // 사용자가 구글 권한 부여 페이지에서 취소
            log.info("OAuth2 로그인 취소")
            redirectStrategy.sendRedirect(request, response, "/")
        } else {
            // 실제 오류 (네트워크, 설정 문제 등)
            log.warn("OAuth2 로그인 실패: ${exception.message}")
            redirectStrategy.sendRedirect(request, response, "/login-error")
        }
    }
}

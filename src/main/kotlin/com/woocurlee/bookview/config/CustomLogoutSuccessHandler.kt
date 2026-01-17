package com.woocurlee.bookview.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomLogoutSuccessHandler : LogoutSuccessHandler {
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        // JWT 쿠키 삭제
        val cookie = jakarta.servlet.http.Cookie("jwt", null)
        cookie.maxAge = 0
        cookie.path = "/"
        response.addCookie(cookie)

        // 홈으로 리다이렉트
        response.sendRedirect("/")
    }
}

package com.woocurlee.bookview.config

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.repository.UserRepository
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val jwtUtil: JwtUtil,
    private val userRepository: UserRepository,
) : SimpleUrlAuthenticationSuccessHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val googleId = oAuth2User.attributes["sub"].toString()
        val email = oAuth2User.attributes["email"].toString()
        val name = oAuth2User.attributes["name"].toString()

        log.info("OAuth2 로그인 성공: googleId=$googleId, email=$email, name=$name")

        // DB에서 사용자 조회
        val user = userRepository.findByGoogleIdAndStatus(googleId, Status.ACTIVE)
        val nickname = user?.nickname ?: name

        // JWT 생성
        val token = jwtUtil.generateToken(googleId, email, nickname)

        // 쿠키에 JWT 저장
        val cookie =
            Cookie("jwt", token).apply {
                isHttpOnly = true
                secure = false // 개발환경에서는 false, 프로덕션에서는 true
                path = "/"
                maxAge = 86400 // 24시간
            }
        response.addCookie(cookie)

        log.info("JWT 토큰 발급 완료")

        // 홈으로 리다이렉트
        redirectStrategy.sendRedirect(request, response, "/")
    }
}

package com.woocurlee.bookview.config

import com.woocurlee.bookview.service.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val authorizationRequestResolver: OAuth2AuthorizationRequestResolver,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val oAuth2FailureHandler: OAuth2FailureHandler,
    private val customLogoutSuccessHandler: CustomLogoutSuccessHandler,
    private val cookieAuthorizationRequestRepository: CookieOAuth2AuthorizationRequestRepository,
) {
    /**
     * 저장 시점 새니타이징(HtmlSanitizer)이 뚫렸을 때 스크립트 실행을 막는 2차 방어선.
     * 인라인 스크립트/이벤트 핸들러를 모두 제거했으므로 script-src에 'unsafe-inline'을 두지 않는다.
     */
    private val contentSecurityPolicy =
        listOf(
            "default-src 'self'",
            // Tailwind CDN, GA4(gtag.js), TipTap 에디터(ESM CDN)
            "script-src 'self' https://cdn.tailwindcss.com https://www.googletagmanager.com https://esm.sh",
            // Tailwind CDN 런타임이 <style>을 논스 없이 주입하므로 'unsafe-inline' 제거 불가
            "style-src 'self' 'unsafe-inline'",
            // 책 표지 원본(fname)은 카카오가 임의의 외부 호스트를 내려줄 수 있어 https 전체를 허용
            "img-src 'self' data: https:",
            "font-src 'self'",
            "connect-src 'self' https://*.google-analytics.com https://*.analytics.google.com https://*.googletagmanager.com",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
        ).joinToString("; ")

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { headers ->
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives(contentSecurityPolicy)
                }
            }.sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        "/books",
                        "/write-review",
                        "/my-page",
                        "/u/**",
                        "/r/**",
                        "/setup-nickname",
                        "/login/**",
                        "/login-error",
                        "/oauth2/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error",
                    ).permitAll()
                    .requestMatchers(
                        "/api/users",
                        "/api/users/db-info",
                        "/api/external/**",
                        "/api/reviews",
                        "/api/comments",
                    ).permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .requestMatchers("/favicon.ico", "/apple-touch-icon.png", "/favicon-*.png")
                    .permitAll()
                    .anyRequest()
                    .permitAll()
            }.oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint { authorization ->
                        authorization
                            .authorizationRequestResolver(authorizationRequestResolver)
                            .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                    }.userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }.successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            }.logout { logout ->
                logout
                    .logoutSuccessHandler(customLogoutSuccessHandler)
                    .deleteCookies("jwt")
                    .permitAll()
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

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
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { session ->
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

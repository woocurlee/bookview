package com.woocurlee.bookview.config

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Base64
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component

@Component
class CookieOAuth2AuthorizationRequestRepository : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    companion object {
        const val COOKIE_NAME = "oauth2_auth_request"
        const val COOKIE_EXPIRE_SECONDS = 180
    }

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        getCookie(request)?.let {
            deserialize(it.value)
        }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizationRequest == null) {
            removeCookie(response)
            return
        }
        val cookie =
            Cookie(COOKIE_NAME, serialize(authorizationRequest)).apply {
                path = "/"
                isHttpOnly = true
                secure = true
                maxAge = COOKIE_EXPIRE_SECONDS
            }
        response.addCookie(cookie)
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        val authRequest = loadAuthorizationRequest(request)
        removeCookie(response)
        return authRequest
    }

    private fun getCookie(request: HttpServletRequest): Cookie? =
        request.cookies?.firstOrNull { it.name == COOKIE_NAME }

    private fun removeCookie(response: HttpServletResponse) {
        val cookie =
            Cookie(COOKIE_NAME, "").apply {
                path = "/"
                isHttpOnly = true
                secure = true
                maxAge = 0
            }
        response.addCookie(cookie)
    }

    private fun serialize(request: OAuth2AuthorizationRequest): String {
        val bytes =
            java.io.ByteArrayOutputStream().use { baos ->
                java.io.ObjectOutputStream(baos).use { it.writeObject(request) }
                baos.toByteArray()
            }
        return Base64.getUrlEncoder().encodeToString(bytes)
    }

    private fun deserialize(cookie: String): OAuth2AuthorizationRequest? =
        try {
            val bytes = Base64.getUrlDecoder().decode(cookie)
            java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes)).use {
                it.readObject() as OAuth2AuthorizationRequest
            }
        } catch (e: Exception) {
            null
        }
}

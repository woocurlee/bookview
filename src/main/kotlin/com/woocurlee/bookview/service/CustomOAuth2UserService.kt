package com.woocurlee.bookview.service

import com.woocurlee.bookview.common.SequenceNames
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.User
import com.woocurlee.bookview.repository.UserRepository
import java.time.LocalDateTime
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val sequenceService: SequenceService,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val googleId = oAuth2User.attributes["sub"].toString()
        val nickname = oAuth2User.attributes["name"] as? String ?: "Unknown"
        val email = oAuth2User.attributes["email"] as? String
        val profileImageUrl = oAuth2User.attributes["picture"] as? String

        var user = userRepository.findByGoogleIdAndStatus(googleId, Status.ACTIVE)

        if (user == null) {
            val userNo = sequenceService.getNextSequence(SequenceNames.USER_SEQ)
            user =
                User(
                    userNo = userNo,
                    googleId = googleId,
                    nickname = nickname,
                    email = email,
                    profileImageUrl = profileImageUrl,
                    status = Status.ACTIVE,
                )
        } else {
            user =
                user.copy(
                    nickname = nickname,
                    email = email,
                    profileImageUrl = profileImageUrl,
                    lastLoginAt = LocalDateTime.now(),
                )
        }

        userRepository.save(user)

        return oAuth2User
    }
}

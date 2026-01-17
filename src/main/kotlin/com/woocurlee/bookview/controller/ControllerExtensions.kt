package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.User
import com.woocurlee.bookview.repository.UserRepository
import org.springframework.ui.Model

/**
 * Principal에서 유저를 조회하고 Model에 추가
 * @return 조회된 User 객체 (없으면 null)
 */
fun addUserToModel(
    principal: Any?,
    userRepository: UserRepository,
    model: Model,
): User? {
    if (principal != null) {
        val attributes = principal as? Map<*, *>
        val googleId = attributes?.get("sub")?.toString()
        if (googleId != null) {
            val user = userRepository.findByGoogleIdAndStatus(googleId, Status.ACTIVE)
            model.addAttribute("user", user)
            return user
        }
    }
    return null
}

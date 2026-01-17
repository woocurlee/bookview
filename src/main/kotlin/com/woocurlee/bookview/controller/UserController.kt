package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.domain.toResponse
import com.woocurlee.bookview.dto.UserResponse
import com.woocurlee.bookview.repository.UserRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class UpdateProfileRequest(
    val nickname: String,
)

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository,
    private val mongoTemplate: MongoTemplate,
) {
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        val users = userRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(users)
    }

    @GetMapping("/db-info")
    fun getDatabaseInfo(): ResponseEntity<Map<String, Any>> {
        val dbName = mongoTemplate.db.name
        val collectionNames = mongoTemplate.db.listCollectionNames().toList()
        val userCount = userRepository.count()

        return ResponseEntity.ok(
            mapOf(
                "database" to dbName,
                "collections" to collectionNames,
                "userCount" to userCount,
            ),
        )
    }

    @PutMapping("/profile")
    fun updateProfile(
        @RequestBody request: UpdateProfileRequest,
        @AuthenticationPrincipal principal: Any,
    ): ResponseEntity<UserResponse> {
        val attributes = principal as Map<*, *>
        val googleId = attributes["sub"].toString()

        val user =
            userRepository.findByGoogleIdAndStatus(googleId, Status.ACTIVE) ?: return ResponseEntity.notFound().build()

        val updatedUser = user.copy(nickname = request.nickname)
        userRepository.save(updatedUser)

        return ResponseEntity.ok(updatedUser.toResponse())
    }
}

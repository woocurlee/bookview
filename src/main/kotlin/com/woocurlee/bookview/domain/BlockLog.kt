package com.woocurlee.bookview.domain

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

enum class TargetType { USER, REVIEW, COMMENT }

enum class BlockAction { BLOCK, UNBLOCK }

/**
 * 어드민(bookview-admin)이 기록하는 차단/해제 이력.
 * bookview 에서는 읽기 전용으로만 접근한다.
 */
@Document(collection = "block_logs")
data class BlockLog(
    @Id
    val id: String? = null,
    val targetType: TargetType,
    val targetId: String,
    val action: BlockAction,
    val reason: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

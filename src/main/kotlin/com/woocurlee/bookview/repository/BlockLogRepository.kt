package com.woocurlee.bookview.repository

import com.woocurlee.bookview.domain.BlockAction
import com.woocurlee.bookview.domain.BlockLog
import com.woocurlee.bookview.domain.TargetType
import org.springframework.data.mongodb.repository.MongoRepository

interface BlockLogRepository : MongoRepository<BlockLog, String> {
    /** 특정 대상의 가장 최근 BLOCK 사유를 가져온다. */
    fun findFirstByTargetTypeAndTargetIdAndActionOrderByCreatedAtDesc(
        targetType: TargetType,
        targetId: String,
        action: BlockAction,
    ): BlockLog?
}

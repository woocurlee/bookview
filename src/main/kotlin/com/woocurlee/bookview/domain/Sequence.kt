package com.woocurlee.bookview.domain

import com.woocurlee.bookview.common.MongoCollections
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = MongoCollections.SEQUENCES)
data class Sequence(
    @Id
    val id: String,
    val seq: Long,
)

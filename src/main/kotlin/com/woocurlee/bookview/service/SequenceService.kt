package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.Sequence
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class SequenceService(
    private val mongoTemplate: MongoTemplate,
) {
    fun getNextSequence(sequenceName: String): Long {
        val query = Query.query(Criteria.where("_id").`is`(sequenceName))
        val update = Update().inc("seq", 1)
        val options = FindAndModifyOptions.options().returnNew(true).upsert(true)

        val sequence =
            mongoTemplate.findAndModify(
                query,
                update,
                options,
                Sequence::class.java,
            )

        return sequence?.seq ?: 1L
    }
}

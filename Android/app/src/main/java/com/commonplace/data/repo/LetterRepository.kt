package com.commonplace.data.repo

import com.commonplace.data.Letter
import com.commonplace.data.dao.LetterDao
import com.commonplace.data.encodeStringArray
import com.commonplace.data.entity.LetterEntity
import com.commonplace.data.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class LetterRepository(private val dao: LetterDao) {

    fun observeAll(): Flow<List<Letter>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun listAll(): List<Letter> =
        dao.listAllSnapshot().map { it.toDomain() }

    suspend fun create(body: String, fragmentsReferenced: List<String>): Letter {
        val id = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        dao.insert(
            LetterEntity(
                id = id,
                body = body,
                fragmentsReferenced = encodeStringArray(fragmentsReferenced),
                createdAt = createdAt,
            )
        )
        return Letter(
            id = id,
            body = body,
            fragmentsReferenced = fragmentsReferenced,
            createdAt = createdAt,
        )
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }
}

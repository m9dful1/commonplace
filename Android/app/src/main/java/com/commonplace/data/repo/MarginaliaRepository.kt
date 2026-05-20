package com.commonplace.data.repo

import com.commonplace.data.Marginalia
import com.commonplace.data.dao.FragmentDao
import com.commonplace.data.dao.MarginaliaDao
import com.commonplace.data.entity.MarginaliaEntity
import com.commonplace.data.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class MarginaliaRepository(
    private val dao: MarginaliaDao,
    @Suppress("unused") private val fragmentDao: FragmentDao,
) {

    fun observeForFragment(fragmentId: String): Flow<List<Marginalia>> =
        dao.observeForFragment(fragmentId).map { rows -> rows.map { it.toDomain() } }

    suspend fun listForFragment(fragmentId: String): List<Marginalia> =
        dao.listForFragment(fragmentId).map { it.toDomain() }

    suspend fun listAll(): List<Marginalia> =
        dao.listAll().map { it.toDomain() }

    suspend fun create(fragmentId: String, body: String, voice: String? = null): Marginalia {
        val id = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        dao.insert(
            MarginaliaEntity(
                id = id,
                fragmentId = fragmentId,
                body = body,
                voice = voice,
                createdAt = createdAt,
            )
        )
        return Marginalia(
            id = id,
            fragmentId = fragmentId,
            body = body,
            voice = voice,
            createdAt = createdAt,
        )
    }
}

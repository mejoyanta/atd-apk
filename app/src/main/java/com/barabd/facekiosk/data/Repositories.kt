package com.barabd.facekiosk.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

class PersonRepository(private val dao: PersonDao) {
    suspend fun all(): List<PersonEntity> = dao.getAll()

    suspend fun findByCode(code: String) = dao.findByCode(code)

    suspend fun save(
        attendanceCode: String,
        displayName: String,
        embedding: FloatArray,
        thumbnailJpeg: ByteArray?
    ): Long {
        val code = attendanceCode.trim()
        val existing = dao.findByCode(code)
        val entity = PersonEntity(
            id = existing?.id ?: 0,
            attendanceCode = code,
            displayName = displayName.trim(),
            embedding = EmbeddingCodec.toBytes(embedding),
            thumbnailJpeg = thumbnailJpeg
        )
        return dao.upsert(entity)
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}

class PunchOutboxRepository(private val dao: PunchOutboxDao) {
    suspend fun enqueue(row: PunchOutboxEntity): Boolean {
        val id = dao.insertIgnore(row)
        return id != -1L
    }

    suspend fun pending() = dao.pending()

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun markAttempt(id: Long, error: String?) = dao.markAttempt(id, error)
}

object EmbeddingCodec {
    fun toBytes(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        embedding.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun fromBytes(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(bytes.size / 4)
        for (i in out.indices) {
            out[i] = buffer.float
        }
        return out
    }
}

package com.barabd.facekiosk.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "people",
    indices = [Index(value = ["attendanceCode"], unique = true)]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attendanceCode: String,
    val displayName: String,
    /** Float embedding serialized as little-endian bytes. */
    val embedding: ByteArray,
    val thumbnailJpeg: ByteArray? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PersonEntity) return false
        return id == other.id && attendanceCode == other.attendanceCode
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(
    tableName = "punch_outbox",
    indices = [Index(value = ["personId", "minuteBucket"], unique = true)]
)
data class PunchOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: String,
    val deviceId: String,
    val punchType: String,
    val time: String,
    val similarity: Double,
    /** yyyy-MM-dd HH:mm for dedupe */
    val minuteBucket: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null
)

package com.barabd.facekiosk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY displayName ASC")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE attendanceCode = :code LIMIT 1")
    suspend fun findByCode(code: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(person: PersonEntity): Long

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PunchOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(row: PunchOutboxEntity): Long

    @Query("SELECT * FROM punch_outbox ORDER BY createdAt ASC LIMIT 50")
    suspend fun pending(): List<PunchOutboxEntity>

    @Query("DELETE FROM punch_outbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE punch_outbox SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markAttempt(id: Long, error: String?)
}

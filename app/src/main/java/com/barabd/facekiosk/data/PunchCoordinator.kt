package com.barabd.facekiosk.data

import com.barabd.facekiosk.net.ErpApiClient
import com.barabd.facekiosk.settings.KioskPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class PunchCoordinator(
    private val prefs: KioskPreferences,
    private val api: ErpApiClient,
    private val outbox: PunchOutboxRepository
) {
    private val lastPunchMs = ConcurrentHashMap<String, Long>()

    fun inCooldown(personId: String): Boolean {
        val last = lastPunchMs[personId] ?: return false
        return System.currentTimeMillis() - last < KioskPreferences.COOLDOWN_MS
    }

    fun markPunched(personId: String) {
        lastPunchMs[personId] = System.currentTimeMillis()
    }

    suspend fun submitPunch(personId: String, similarity: Double): PunchSubmitResult {
        if (inCooldown(personId)) {
            return PunchSubmitResult.Cooldown
        }
        val time = formatNow()
        val minuteBucket = time.substring(0, 16) // yyyy-MM-dd HH:mm
        val row = PunchOutboxEntity(
            personId = personId,
            deviceId = prefs.deviceId,
            punchType = prefs.punchType,
            time = time,
            similarity = similarity,
            minuteBucket = minuteBucket
        )
        val enqueued = outbox.enqueue(row)
        if (!enqueued) {
            // Duplicate person+minute — treat as cooldown/dedupe
            markPunched(personId)
            return PunchSubmitResult.Cooldown
        }
        markPunched(personId)
        flushPending()
        return PunchSubmitResult.Accepted(time)
    }

    suspend fun flushPending() {
        val pending = outbox.pending()
        for (row in pending) {
            val result = api.punch(
                deviceId = row.deviceId,
                personId = row.personId,
                time = row.time,
                punchType = row.punchType,
                similarity = row.similarity
            )
            if (result.ok || result.code == 403) {
                // 403 won't succeed on retry without ops change — drop to avoid infinite loop
                outbox.delete(row.id)
            } else {
                outbox.markAttempt(row.id, result.message)
            }
        }
    }

    companion object {
        private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        fun formatNow(): String = synchronized(fmt) { fmt.format(Date()) }
    }
}

sealed class PunchSubmitResult {
    data class Accepted(val time: String) : PunchSubmitResult()
    data object Cooldown : PunchSubmitResult()
}

package com.barabd.facekiosk.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KioskPreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kiosk_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var serverBaseUrl: String
        get() = prefs.getString(KEY_SERVER, "")?.trim()?.trimEnd('/') ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER, value.trim().trimEnd('/')).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE, value.trim()).apply()

    var terminalSn: String
        get() = prefs.getString(KEY_SN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SN, value.trim()).apply()

    /** When true, punch_type = check_in; else check_out. */
    var punchAsCheckIn: Boolean
        get() = prefs.getBoolean(KEY_CHECK_IN, true)
        set(value) = prefs.edit().putBoolean(KEY_CHECK_IN, value).apply()

    val punchType: String
        get() = if (punchAsCheckIn) "check_in" else "check_out"

    companion object {
        const val IDENTIFY_THRESHOLD = 0.80f
        const val LIVENESS_THRESHOLD = 0.50f
        const val COOLDOWN_MS = 60_000L
        const val ENROLL_SHOTS = 3

        private const val KEY_SERVER = "server_base_url"
        private const val KEY_DEVICE = "device_id"
        private const val KEY_SN = "terminal_sn"
        private const val KEY_CHECK_IN = "punch_check_in"
    }
}

package com.barabd.facekiosk.net

import com.barabd.facekiosk.settings.KioskPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ApiResult(
    val ok: Boolean,
    val code: Int,
    val body: String,
    val message: String
)

class ErpApiClient(private val prefs: KioskPreferences) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun health(): ApiResult {
        val base = prefs.serverBaseUrl
        if (base.isBlank()) {
            return ApiResult(false, 0, "", "Server Base URL is empty")
        }
        val request = Request.Builder()
            .url("$base/kby-ai/health")
            .get()
            .build()
        return execute(request)
    }

    fun punch(
        deviceId: String = prefs.deviceId,
        personId: String,
        time: String,
        punchType: String = prefs.punchType,
        similarity: Double
    ): ApiResult {
        val base = prefs.serverBaseUrl
        if (base.isBlank()) {
            return ApiResult(false, 0, "", "Server Base URL is empty")
        }
        if (deviceId.isBlank()) {
            return ApiResult(false, 0, "", "Device ID is empty")
        }
        val payload = JSONObject()
            .put("device_id", deviceId)
            .put("person_id", personId)
            .put("time", time)
            .put("punch_type", punchType)
            .put("similarity", similarity)
            .toString()
        val request = Request.Builder()
            .url("$base/kby-ai/punch")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        return execute(request)
    }

    private fun execute(request: Request): ApiResult {
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val message = when {
                    response.isSuccessful -> "OK"
                    response.code == 403 -> "Forbidden (device/CIDR) — HTTP 403"
                    else -> "HTTP ${response.code}"
                }
                ApiResult(response.isSuccessful, response.code, body, message)
            }
        } catch (e: Exception) {
            ApiResult(false, 0, "", e.message ?: "Network error")
        }
    }
}

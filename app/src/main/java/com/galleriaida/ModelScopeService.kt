package com.galleriaida.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * ModelScope is an ASYNC image provider:
 *   1. POST a generation request  → returns a task_id
 *   2. Poll the task endpoint every [POLL_INTERVAL_MS] until task_status == "SUCCEED"
 *   3. Download the PNG from output_images[0]
 *
 * Poll timeout is [MAX_POLL_ATTEMPTS] × [POLL_INTERVAL_MS] (≈ 4 minutes).
 */
object ModelScopeService {

    private const val TAG = "MODELSCOPE"

    private const val BASE_URL       = "https://api-inference.modelscope.ai/v1"
    private const val POLL_INTERVAL_MS = 10_000L    // 10 seconds
    private const val MAX_POLL_ATTEMPTS = 24         // 24 × 10s = 4 minutes

    // ── Token test ────────────────────────────────────────────────────────────

    /**
     * Validates the token by GETting /models. Returns Pair(isValid, rawJson).
     * The JSON is stored so the user can view it (like the Gemini models list).
     */
    suspend fun validateAndFetchModels(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$BASE_URL/models").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout    = 30_000
                requestMethod  = "GET"
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            conn.connect()
            val code = conn.responseCode
            val body = if (code == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()
            Log.d(TAG, "validateAndFetchModels HTTP $code")
            Pair(code == 200, if (code == 200) body else "")
        } catch (e: Exception) {
            Log.e(TAG, "validateAndFetchModels error: ${e.message}")
            Pair(false, "")
        }
    }

    // ── Submit → poll → download ────────────────────────────────────────────────

    /**
     * Runs the full async flow and returns the downloaded PNG file, or null on failure/timeout.
     */
    suspend fun generateImage(
        context: Context,
        englishPrompt: String,
        model: String,
        apiKey: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val taskId = submitTask(englishPrompt, model, apiKey) ?: return@withContext null
            Log.d(TAG, "Task submitted: $taskId — polling every ${POLL_INTERVAL_MS}ms")

            var attempt = 0
            while (attempt < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempt++

                val imageUrl = pollTask(taskId, apiKey)
                if (imageUrl == PENDING) {
                    Log.d(TAG, "Poll $attempt/$MAX_POLL_ATTEMPTS — still running")
                    continue
                }
                if (imageUrl.isNullOrBlank()) {
                    Log.e(TAG, "Poll $attempt — task failed or returned no image")
                    return@withContext null
                }
                // Success — download the PNG
                Log.d(TAG, "Image ready after $attempt polls: $imageUrl")
                return@withContext downloadImage(context, imageUrl, model)
            }
            Log.e(TAG, "Timed out after $MAX_POLL_ATTEMPTS polls (~4 min)")
            null
        } catch (e: Exception) {
            Log.e(TAG, "generateImage error: ${e.message}", e)
            null
        }
    }

    // Sentinel meaning "task still running"
    private const val PENDING = "__PENDING__"

    private fun submitTask(prompt: String, model: String, apiKey: String): String? {
        return try {
            val payload = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("n", 1)
                put("size", "768x1200")
                put("seed", -1)
            }
            val conn = (URL("$BASE_URL/images/generations").openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout    = 60_000
                requestMethod  = "POST"
                doOutput       = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-ModelScope-Async-Mode", "true")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()

            if (code !in 200..299) {
                Log.e(TAG, "submitTask HTTP $code — $body")
                return null
            }
            val taskId = JSONObject(body).optString("task_id", "")
            taskId.ifBlank { null }
        } catch (e: Exception) {
            Log.e(TAG, "submitTask error: ${e.message}", e)
            null
        }
    }

    /**
     * Polls once. Returns:
     *   - the image URL string on SUCCEED
     *   - [PENDING] while still running
     *   - null on failure
     */
    private fun pollTask(taskId: String, apiKey: String): String? {
        return try {
            val conn = (URL("$BASE_URL/tasks/$taskId").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout    = 30_000
                requestMethod  = "GET"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("X-ModelScope-Task-Type", "image_generation")
            }
            conn.connect()
            val code = conn.responseCode
            val body = if (code == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()

            if (code != 200) {
                Log.e(TAG, "pollTask HTTP $code — $body")
                return null
            }

            val json   = JSONObject(body)
            val status = json.optString("task_status", "")
            when (status) {
                "SUCCEED" -> {
                    val images = json.optJSONArray("output_images")
                    if (images != null && images.length() > 0) images.getString(0) else null
                }
                "FAILED", "CANCELED" -> {
                    Log.e(TAG, "pollTask task_status=$status")
                    null
                }
                else -> PENDING   // RUNNING / PENDING / empty
            }
        } catch (e: Exception) {
            Log.e(TAG, "pollTask error: ${e.message}", e)
            null
        }
    }

    private fun downloadImage(context: Context, imageUrl: String, model: String): File? {
        return try {
            val safeModel = model.replace("/", "_")
            val file = File(context.cacheDir, "modelscope_${safeModel}_${System.currentTimeMillis()}.png")
            val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout    = 120_000
                requestMethod  = "GET"
            }
            conn.connect()
            val code = conn.responseCode
            if (code == 200) {
                conn.inputStream.use { i -> file.outputStream().use { o -> i.copyTo(o) } }
                conn.disconnect()
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Downloaded ${file.length()} bytes")
                    file
                } else {
                    Log.e(TAG, "Downloaded file empty")
                    null
                }
            } else {
                Log.e(TAG, "downloadImage HTTP $code")
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadImage error: ${e.message}", e)
            null
        }
    }
}
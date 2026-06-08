package com.galleriaida.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PollinationsService {

    private const val TAG = "POLLINATIONS"

    // ── Ping / validate key ──────────────────────────────────────────────────

    suspend fun pingTest(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val keyParam = if (apiKey.isNotBlank()) "&key=$apiKey" else ""
            val url  = URL("https://gen.pollinations.ai/image/test?model=flux&width=128&height=128&seed=1&enhance=false$keyParam")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout    = 30_000
            conn.requestMethod  = "GET"
            conn.connect()
            val code = conn.responseCode
            Log.d(TAG, "Ping response: $code")
            conn.disconnect()
            code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed: ${e.message}")
            false
        }
    }

    // ── Generate image with one specific model ───────────────────────────────

    suspend fun generateImage(
        context: Context,
        englishPrompt: String,
        model: String,
        apiKey: String
    ): File? = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== generateImage model=$model ===")
        try {
            val sanitized = englishPrompt.replace("\"", " ").replace("'", " ").trim()
            val encoded   = URLEncoder.encode(sanitized, "UTF-8").replace("+", "%20")
            val seed      = (1..999_999).random()
            val keyParam  = if (apiKey.isNotBlank()) "&key=$apiKey" else ""
            val urlString = "https://gen.pollinations.ai/image/$encoded?model=$model&width=768&height=1200&seed=$seed&enhance=false$keyParam"

            Log.d(TAG, "──────────────────────────────────────────")
            Log.d(TAG, "MODEL   : $model")
            Log.d(TAG, "PROMPT  : $sanitized")
            Log.d(TAG, "URL     : $urlString")
            Log.d(TAG, "API KEY : ${if (apiKey.isBlank()) "NONE (free tier)" else "${apiKey.take(6)}… (len=${apiKey.length})"}")
            Log.d(TAG, "──────────────────────────────────────────")

            val file = File(context.cacheDir, "pollinations_${model}_${System.currentTimeMillis()}.jpg")
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 30_000
            conn.readTimeout    = 60_000
            conn.requestMethod  = "GET"
            conn.connect()

            val code = conn.responseCode
            Log.d(TAG, "HTTP $code for model=$model")

            if (code == 200) {
                conn.inputStream.use { i -> file.outputStream().use { o -> i.copyTo(o) } }
                conn.disconnect()
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Success with model=$model  size=${file.length()}")
                    return@withContext file
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                Log.e(TAG, "HTTP $code — $err")
                conn.disconnect()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "generateImage error model=$model: ${e.message}", e)
            null
        }
    }

    // ── Try 3 models in order, return first success ──────────────────────────

    suspend fun generateImageWithFallbacks(
        context: Context,
        englishPrompt: String,
        model1: String,
        model2: String,
        model3: String,
        apiKey: String,
        onModelSwitch: (modelName: String) -> Unit = {}
    ): Pair<File, String>? {
        for ((idx, model) in listOf(model1, model2, model3).filter { it.isNotBlank() }.withIndex()) {
            if (idx > 0) onModelSwitch(model)
            Log.d(TAG, "Trying model: $model")
            val file = generateImage(context, englishPrompt, model, apiKey)
            if (file != null) return Pair(file, model)
            Log.w(TAG, "Model $model failed, trying next…")
        }
        Log.e(TAG, "All Pollinations models failed.")
        return null
    }
}
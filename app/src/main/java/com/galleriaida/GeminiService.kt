package com.galleriaida.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody

class GeminiService {

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15L, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30L, java.util.concurrent.TimeUnit.SECONDS)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                return okhttp3.Dns.SYSTEM.lookup(hostname)
                    .filter { it is java.net.Inet4Address }
            }
        })
        .build()

    private fun generateContentUrl(apiKey: String, model: String) =
        "https://generativelanguage.googleapis.com/v1beta/$model:generateContent?key=$apiKey"

    private fun predictUrl(apiKey: String, model: String) =
        "https://generativelanguage.googleapis.com/v1beta/$model:predict?key=$apiKey"

    private fun modelsUrl(apiKey: String) =
        "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"

    // ── Validation + model list ──────────────────────────────────────────────

    suspend fun validateAndFetchModels(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(modelsUrl(apiKey))
            .get()
            .addHeader("Accept", "application/json")
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val valid = JSONObject(body).has("models")
                    Log.d("GeminiService", "Validation: $valid")
                    return@withContext Pair(valid, if (valid) body else "")
                }
                return@withContext Pair(false, "")
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Validation error: ${e.localizedMessage}")
            return@withContext Pair(false, "")
        }
    }

    suspend fun validateKey(apiKey: String): Boolean {
        return validateAndFetchModels(apiKey).first
    }

    // ── Auto model selection ─────────────────────────────────────────────────

    fun selectBestModels(modelsJson: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val arr = JSONObject(modelsJson).getJSONArray("models")
            val textModels = mutableListOf<String>()
            val imageModels = mutableListOf<String>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("name")
                val methods = obj.getJSONArray("supportedGenerationMethods")
                    .let { m -> (0 until m.length()).map { m.getString(it) } }
                if ("generateContent" in methods) textModels.add(name)
                if ("predict" in methods) imageModels.add(name)
            }

            val textPriority = listOf(
                "models/gemini-3.5-flash",
                "models/gemini-3.1-flash-lite",
                "models/gemini-2.5-flash",
                "models/gemini-2.0-flash"
            )
            val imagePriority = listOf(
                "models/imagen-4.0-ultra-generate-001",
                "models/imagen-4.0-generate-001",
                "models/imagen-4.0-fast-generate-001"
            )

            val bestText = textPriority.firstOrNull { it in textModels }
                ?: textModels.filterNot { "preview" in it.lowercase() }.firstOrNull()
                ?: textModels.firstOrNull() ?: ""

            val bestImage = imagePriority.firstOrNull { it in imageModels }
                ?: imageModels.filterNot { "preview" in it.lowercase() }.firstOrNull()
                ?: imageModels.firstOrNull() ?: ""

            result["questions"] = bestText
            result["translation"] = bestText
            result["imagePrompt"] = bestText
            result["imageGeneration"] = bestImage

            Log.d("GeminiService", "Auto-selected text: $bestText, image: $bestImage")
        } catch (e: Exception) {
            Log.e("GeminiService", "Model selection error: ${e.message}")
        }
        return result
    }

    // ── Text generation using OkHttp with strict JSON output ─────────────────

    private fun postGenerateContent(apiKey: String, model: String, prompt: String): String {
        val url = generateContentUrl(apiKey, model)

        // 1. Force response structure to application/json
        val generationConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
        }

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put(
                    "parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    )
                )
            ))
            put("generationConfig", generationConfig)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, payload.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                return responseBody
            } else {
                Log.e("GeminiService", "HTTP ${response.code}: $responseBody")
                if (response.code == 429) {
                    throw Exception("API quota exceeded. Please wait a few seconds or choose another text model in settings.")
                }
                throw Exception("Server returned ${response.code}: $responseBody")
            }
        }
    }

    private fun extractText(responseJson: String): String {
        return JSONObject(responseJson)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    // ── Math questions ───────────────────────────────────────────────────────

    suspend fun generateMathQuestions(
        apiKey: String,
        model: String,
        language: String,
        count: Int = 10
    ): Result<List<MathQuestion>> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Generate $count math questions for children aged 6-12, in $language language.
                Mix of addition, subtraction, and simple multiplication.
                Return ONLY a JSON array, no markdown, in this exact format:
                [{"question":"2 + 3 = ?","answer":5,"difficulty":1},...]
                difficulty: 1=easy(1 star), 2=medium(2 stars), 3=hard(3 stars)
            """.trimIndent()

            val response = postGenerateContent(apiKey, model, prompt)
            val text = extractText(response)
            val cleaned = text.trim().removePrefix("```json").removeSuffix("```").trim()
            val jsonArray = JSONArray(cleaned)
            val questions = mutableListOf<MathQuestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                questions.add(MathQuestion(
                    question = obj.getString("question"),
                    answer = obj.getInt("answer"),
                    difficulty = obj.getInt("difficulty")
                ))
            }
            Result.success(questions)
        } catch (e: Exception) {
            Log.e("GeminiService", "generateMathQuestions error: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Phrase generation ────────────────────────────────────────────────────

    suspend fun generatePhrase(
        apiKey: String,
        model: String,
        character: String,
        action: String,
        place: String,
        language: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = "I want you to creatively combine the words $character, $action, and $place into a simple, child-friendly phrase in $language. This phrase will then be used as the prompt to generate an image suitable for a kid. Return only one phrase and return it in a json formatted like this { \"phrase\": \"\" }"
            Log.d("GALLERIA_AI", "=== PHRASE REQUEST ===")
            Log.d("GALLERIA_AI", "Model: $model")
            Log.d("GALLERIA_AI", "Prompt: $prompt")

            val response = postGenerateContent(apiKey, model, prompt)
            val text = extractText(response)
            val cleaned = text.trim().removePrefix("```json").removeSuffix("```").trim()
            val phrase = JSONObject(cleaned).getString("phrase")

            Log.d("GALLERIA_AI", "=== PHRASE RESPONSE ===")
            Log.d("GALLERIA_AI", "Phrase: $phrase")
            Result.success(phrase)
        } catch (e: Exception) {
            Log.e("GeminiService", "generatePhrase error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Image generation using OkHttp (Adaptive format) ──────────────────────

    suspend fun generateImage(
        apiKey: String,
        model: String,
        imagePrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isGeminiImageModel = model.contains("gemini")
            val targetUrl = if (isGeminiImageModel) generateContentUrl(apiKey, model) else predictUrl(apiKey, model)

            val payload = if (isGeminiImageModel) {
                // Modern Gemini Image format payload
                JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", imagePrompt)
                        ))
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put("IMAGE"))
                    })
                }
            } else {
                // Legacy Vertex Imagen payload
                JSONObject()
                    .put("instances", JSONArray().put(
                        JSONObject().put("prompt", imagePrompt)
                    ))
                    .put("parameters", JSONObject()
                        .put("sampleCount", 1)
                        .put("aspectRatio", "1:1")
                        .put("outputMimeType", "image/jpeg")
                    )
            }

            Log.d("GALLERIA_AI", "=== IMAGE REQUEST ===")
            Log.d("GALLERIA_AI", "Model: $model")
            Log.d("GALLERIA_AI", "Image prompt: $imagePrompt")

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = RequestBody.create(mediaType, payload.toString())

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")

            if (!isGeminiImageModel) {
                requestBuilder.addHeader("x-goog-api-key", apiKey)
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val base64 = if (isGeminiImageModel) {
                        JSONObject(responseBody)
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getJSONObject("inlineData")
                            .getString("data")
                    } else {
                        val predictions = JSONObject(responseBody).getJSONArray("predictions")
                        predictions.getJSONObject(0).getString("bytesBase64Encoded")
                    }
                    Log.d("GELLERIA_AI", "=== IMAGE RESPONSE ===")
                    Log.d("GELLERIA_AI", "Base64 length: ${base64.length} chars")
                    Result.success(base64)
                } else {
                    Log.e("GeminiService", "Image HTTP ${response.code}: $responseBody")
                    if (response.code == 429) {
                        Result.failure(Exception("Image API quota exceeded. Please wait a few seconds before trying again."))
                    } else {
                        Result.failure(Exception("Image generation failed: ${response.code}"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "generateImage error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── UI translation ───────────────────────────────────────────────────────

    /**
     * Translates only the provided [keys] map (key → englishDefault) into [language].
     * Returns a map of key → translatedValue for all successfully translated keys.
     */
    suspend fun translateKeys(
        apiKey: String,
        model: String,
        language: String,
        keys: Map<String, String>
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val inputJson = org.json.JSONObject()
            keys.forEach { (k, v) -> inputJson.put(k, v) }

            val prompt = """
Translate every value in the following JSON object into $language.
Rules:
- Keep all placeholder tokens (%s, %d, %1${'$'}s, etc.) exactly as-is.
- Keep all emoji exactly as-is.
- Do NOT add, remove, or rename any keys.
- Return ONLY a valid JSON object with the same keys, no markdown, no explanation.

Input JSON:
$inputJson
            """.trimIndent()

            Log.d("GeminiService", "translateKeys → model=$model lang=$language keys=${keys.size}")

            val response = postGenerateContent(apiKey, model, prompt)
            val text = extractText(response)
            val cleaned = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val j = org.json.JSONObject(cleaned)

            val result = mutableMapOf<String, String>()
            keys.keys.forEach { key ->
                if (j.has(key)) result[key] = j.getString(key)
            }
            Log.d("GeminiService", "translateKeys success: ${result.size} keys translated")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("GeminiService", "translateKeys error: ${e.message}", e)
            Result.failure(e)
        }
    }

}

data class MathQuestion(
    val question: String,
    val answer: Int,
    val difficulty: Int
)

data class ImageMeta(
    val title: String,
    val sentence: String,
    val imagePrompt: String
)
package com.gelleriaida.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
        val request = okhttp3.Request.Builder()
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

    // Keep old validateKey for compatibility
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
                ?: textModels.firstOrNull()
                ?: ""

            val bestImage = imagePriority.firstOrNull { it in imageModels }
                ?: imageModels.filterNot { "preview" in it.lowercase() }.firstOrNull()
                ?: imageModels.firstOrNull()
                ?: ""

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

    // ── Text generation (generateContent) ───────────────────────────────────

    private fun postGenerateContent(apiKey: String, model: String, prompt: String): String {
        val url = URL(generateContentUrl(apiKey, model))
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true

            val payload = JSONObject().put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts", JSONArray().put(
                            JSONObject().put("text", prompt)
                        )
                    )
                )
            )
            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

            return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e("GeminiService", "HTTP ${connection.responseCode}: $error")
                throw Exception("Server returned ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
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

    suspend fun generateImagePromptAndMeta(
        apiKey: String,
        model: String,
        words: List<String>,
        language: String
    ): Result<ImageMeta> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are helping create a reward image for a child.
                Use these words: ${words.joinToString(", ")}.
                Respond ONLY with a JSON object, no markdown, in this exact format:
                {"title":"Short fun title in $language","sentence":"One cheerful sentence in $language describing the scene","imagePrompt":"A colorful friendly cartoon illustration of [scene], suitable for children, bright vivid colors, no text in image"}
            """.trimIndent()

            val response = postGenerateContent(apiKey, model, prompt)
            val text = extractText(response)
            val cleaned = text.trim().removePrefix("```json").removeSuffix("```").trim()
            val obj = JSONObject(cleaned)
            Result.success(ImageMeta(
                title = obj.getString("title"),
                sentence = obj.getString("sentence"),
                imagePrompt = obj.getString("imagePrompt")
            ))
        } catch (e: Exception) {
            Log.e("GeminiService", "generateImagePromptAndMeta error: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Image generation (predict / Imagen) ──────────────────────────────────

    suspend fun generateImage(
        apiKey: String,
        model: String,
        imagePrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(predictUrl(apiKey, model))
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("x-goog-api-key", apiKey)
                connection.connectTimeout = 60000
                connection.readTimeout = 60000
                connection.doOutput = true

                val payload = JSONObject()
                    .put("instances", JSONArray().put(
                        JSONObject().put("prompt", imagePrompt)
                    ))
                    .put("parameters", JSONObject()
                        .put("sampleCount", 1)
                        .put("aspectRatio", "1:1")
                        .put("outputMimeType", "image/jpeg")
                    )

                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val predictions = JSONObject(responseText).getJSONArray("predictions")
                    val base64 = predictions.getJSONObject(0).getString("bytesBase64Encoded")
                    Result.success(base64)
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("GeminiService", "Imagen HTTP ${connection.responseCode}: $error")
                    Result.failure(Exception("Image generation failed: ${connection.responseCode}"))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "generateImage error: ${e.message}")
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

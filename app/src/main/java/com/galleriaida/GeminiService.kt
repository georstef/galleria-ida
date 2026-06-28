package com.galleriaida.network

import android.content.Context
import android.util.Log
import com.galleriaida.data.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody

class GeminiService {

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15L, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> =
                okhttp3.Dns.SYSTEM.lookup(hostname).filter { it is java.net.Inet4Address }
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
                    val body  = response.body?.string() ?: ""
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

    suspend fun validateKey(apiKey: String): Boolean = validateAndFetchModels(apiKey).first

    // ── Auto model selection ─────────────────────────────────────────────────

    fun selectBestModels(modelsJson: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val arr         = JSONObject(modelsJson).getJSONArray("models")
            val textModels  = mutableListOf<String>()
            val imageModels = mutableListOf<String>()

            for (i in 0 until arr.length()) {
                val obj     = arr.getJSONObject(i)
                val name    = obj.getString("name")
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

            result["questions"]       = bestText
            result["translation"]     = bestText
            result["imagePrompt"]     = bestText
            result["imageGeneration"] = bestImage

            Log.d("GeminiService", "Auto-selected text: $bestText, image: $bestImage")
        } catch (e: Exception) {
            Log.e("GeminiService", "Model selection error: ${e.message}")
        }
        return result
    }

    // ── Text generation (internal) ───────────────────────────────────────────

    private fun postGenerateContent(apiKey: String, model: String, prompt: String): String {
        val generationConfig = JSONObject().apply { put("responseMimeType", "application/json") }
        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            put("generationConfig", generationConfig)
        }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), payload.toString())
        val request = Request.Builder()
            .url(generateContentUrl(apiKey, model))
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) return responseBody
            Log.e("GeminiService", "HTTP ${response.code}: $responseBody")
            if (response.code == 429)
                throw Exception("API quota exceeded. Please wait a few seconds or choose another text model in settings.")
            throw Exception("Server returned ${response.code}: $responseBody")
        }
    }

    private fun extractText(responseJson: String): String =
        JSONObject(responseJson)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

    // ── Quiz questions ───────────────────────────────────────────────────────

    companion object {
        private const val QUIZ_QUESTION_COUNT = 10

        // Maps the integer stored in Player.schoolYearPosition to the
        // English phrase the prompt expects
        fun schoolYearPositionLabel(position: String): String = when (position.trim()) {
            "beginning" -> "beginning of the school year"
            "middle"    -> "middle of the school year"
            "end"       -> "end of the school year"
            else -> "middle of the school year"   // safe fallback
        }
    }

    /**
     * Loads quiz_prompt.txt from assets, substitutes all placeholders,
     * sends to Gemini, and parses the returned JSON array into [QuizQuestion]s.
     */
    suspend fun generateQuizQuestions(
        context: Context,
        apiKey: String,
        model: String,
        language: String,
        grade: String,
        level: String
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        try {
            // Load prompt template from assets
            val template = context.assets.open("quiz_prompt.txt")
                .bufferedReader()
                .use { it.readText() }

            // Substitute all placeholders
            val prompt = template
                .replace("{{player_language}}",    language)
                .replace("{{selection}}",          QUIZ_QUESTION_COUNT.toString())
                .replace("{{school_year_position}}", schoolYearPositionLabel(level))
                .replace("{{player_class}}",       grade)

            Log.d("GeminiService", "generateQuizQuestions → model=$model lang=$language grade=$grade level=$level")
            Log.d("GALLERIA_AI", "Resolved quiz prompt:\n$prompt")

            val response = postGenerateContent(apiKey, model, prompt)
            val text     = extractText(response)

            // Strip any accidental markdown fences the model may have added
            val cleaned = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleaned)
            val questions = mutableListOf<QuizQuestion>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // options — only present for multiple_choice
                val optionsArray = obj.optJSONArray("options")
                val options: List<String>? = if (optionsArray != null) {
                    (0 until optionsArray.length()).map { optionsArray.getString(it) }
                } else {
                    null
                }

                questions.add(
                    QuizQuestion(
                        id          = java.util.UUID.randomUUID().toString(),
                        subject     = obj.optString("subject", ""),
                        category    = obj.optString("category", ""),
                        level       = obj.optInt("level", 1),
                        type        = obj.optString("type", "text"),
                        instruction = obj.optString("instruction", ""),
                        question    = obj.getString("question"),
                        options     = options,
                        answer      = obj.getString("answer")
                    )
                )
            }

            Log.d("GeminiService", "generateQuizQuestions → parsed ${questions.size} questions")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e("GeminiService", "generateQuizQuestions error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Phrase + title generation ─────────────────────────────────────────────

    data class GeminiPhrases(
        val phraseEn: String,
        val titleEn: String,
        val phrasePlayer: String,
        val titlePlayer: String
    )

    suspend fun generatePhrase(
        apiKey: String,
        model: String,
        character: String,
        action: String,
        place: String,
        language: String
    ): Result<GeminiPhrases> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
Write a short, imaginative, child-friendly descriptive paragraph that naturally incorporates the words $character, $action, and $place. The paragraph should be suitable as an image-generation prompt for children aged 7–12 and contain only wholesome, age-appropriate content. The prompt must force the ai that will create the image to use vibrant and ultra vivid colors. From that paragraph create a short title for the image. Return the title and the paragraph, in english and the same two values in $language in valid JSON using the following format:
{
  "title_en": "",
  "phrase_en": "",
  "title_local": "",
  "phrase_local": ""
}
            """.trimIndent()

            Log.d("GALLERIA_AI", "=== PHRASE REQUEST ===")
            Log.d("GALLERIA_AI", "Model: $model  Language: $language")

            val response = postGenerateContent(apiKey, model, prompt)
            val text     = extractText(response)
            val cleaned  = text.trim().removePrefix("```json").removeSuffix("```").trim()

            val j = JSONObject(cleaned)
            val result = GeminiPhrases(
                phraseEn     = j.getString("phrase_en"),
                titleEn      = j.getString("title_en"),
                phrasePlayer = j.getString("phrase_local"),
                titlePlayer  = j.getString("title_local")
            )

            Log.d("GALLERIA_AI", "phraseEn=${result.phraseEn}")
            Log.d("GALLERIA_AI", "titleEn=${result.titleEn}")
            Log.d("GALLERIA_AI", "phrasePlayer=${result.phrasePlayer}")
            Log.d("GALLERIA_AI", "titlePlayer=${result.titlePlayer}")

            Result.success(result)
        } catch (e: Exception) {
            Log.e("GeminiService", "generatePhrase error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Image generation (Gemini / Imagen) ───────────────────────────────────

    suspend fun generateImage(
        apiKey: String,
        model: String,
        imagePrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isGeminiImageModel = model.contains("gemini")
            val targetUrl = if (isGeminiImageModel) generateContentUrl(apiKey, model) else predictUrl(apiKey, model)

            val payload = if (isGeminiImageModel) {
                JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", imagePrompt)))
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put("IMAGE"))
                    })
                }
            } else {
                JSONObject()
                    .put("instances", JSONArray().put(JSONObject().put("prompt", imagePrompt)))
                    .put("parameters", JSONObject()
                        .put("sampleCount", 1)
                        .put("aspectRatio", "2:3")
                        .put("outputMimeType", "image/jpeg"))
            }

            Log.d("GALLERIA_AI", "=== IMAGE REQUEST === model=$model")

            val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), payload.toString())
            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")

            if (!isGeminiImageModel) requestBuilder.addHeader("x-goog-api-key", apiKey)

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val base64 = if (isGeminiImageModel) {
                        JSONObject(responseBody)
                            .getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts").getJSONObject(0)
                            .getJSONObject("inlineData").getString("data")
                    } else {
                        JSONObject(responseBody).getJSONArray("predictions")
                            .getJSONObject(0).getString("bytesBase64Encoded")
                    }
                    Log.d("GALLERIA_AI", "Image base64 length: ${base64.length}")
                    Result.success(base64)
                } else {
                    Log.e("GeminiService", "Image HTTP ${response.code}: $responseBody")
                    if (response.code == 429)
                        Result.failure(Exception("Image API quota exceeded. Please wait before trying again."))
                    else
                        Result.failure(Exception("Image generation failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "generateImage error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── UI translation ───────────────────────────────────────────────────────

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

            val response = postGenerateContent(apiKey, model, prompt)
            val text     = extractText(response)
            val cleaned  = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val j        = org.json.JSONObject(cleaned)
            val result   = mutableMapOf<String, String>()
            keys.keys.forEach { key -> if (j.has(key)) result[key] = j.getString(key) }
            Log.d("GeminiService", "translateKeys success: ${result.size} keys")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("GeminiService", "translateKeys error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Word list translation ─────────────────────────────────────────────────

    data class TranslatedWordLists(
        val characters: List<String>,
        val actions: List<String>,
        val places: List<String>
    )

    suspend fun translateWordLists(
        apiKey: String,
        model: String,
        language: String,
        characters: List<String>,
        actions: List<String>,
        places: List<String>
    ): Result<TranslatedWordLists> = withContext(Dispatchers.IO) {
        try {
            val payload = org.json.JSONObject().apply {
                put("characters", org.json.JSONArray(characters))
                put("actions",    org.json.JSONArray(actions))
                put("places",     org.json.JSONArray(places))
            }

            val prompt = """
Translate every word/phrase in the following JSON into $language.
Rules:
- Keep the same JSON structure with keys "characters", "actions", "places".
- Each array must have EXACTLY the same number of items as the input.
- Translate each item at the same index — do not reorder, skip, or merge items.
- Return ONLY a valid JSON object, no markdown, no explanation.

Input:
$payload
            """.trimIndent()

            Log.d("GeminiService", "translateWordLists → model=$model language=$language items=${characters.size}+${actions.size}+${places.size}")

            val response = postGenerateContent(apiKey, model, prompt)
            val text     = extractText(response)
            val cleaned  = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val j        = org.json.JSONObject(cleaned)

            fun parseArray(key: String, fallback: List<String>): List<String> {
                val arr = j.optJSONArray(key) ?: return fallback
                return (0 until arr.length()).map { arr.getString(it) }
            }

            Result.success(TranslatedWordLists(
                characters = parseArray("characters", characters),
                actions    = parseArray("actions",    actions),
                places     = parseArray("places",     places)
            ))
        } catch (e: Exception) {
            Log.e("GeminiService", "translateWordLists error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
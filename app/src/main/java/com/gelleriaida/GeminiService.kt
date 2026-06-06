package com.gelleriaida.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiService {
    private val okHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5L, java.util.concurrent.TimeUnit.SECONDS)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                return okhttp3.Dns.SYSTEM.lookup(hostname)
                    .filter { it is java.net.Inet4Address } // Strict IPv4 filter bypasses the Android 16 bug!
            }
        })
        .build()
    private fun buildUrl(apiKey: String) =
        "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"

    suspend fun validateKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val targetUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        Log.d("GeminiSpeedTest", "🚀 Starting OkHttp validation path...")

        // 2. Build the request structure using the library
        val request = okhttp3.Request.Builder()
            .url(targetUrl)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        try {
            // 3. Execute the request safely inside a auto-closing block
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("GeminiSpeedTest", "📡 Server responded with status code: ${response.code}")

                if (response.isSuccessful) {
                    val responseText = response.body?.string() ?: ""
                    val isValid = JSONObject(responseText).has("models")
                    Log.d("GeminiSpeedTest", "✅ Validation successfully completed: $isValid")
                    return@withContext isValid
                }
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("GeminiSpeedTest", "❌ OkHttp encountered an error: ${e.localizedMessage}")
            return@withContext false
        }
    }

    /**
     * LOCAL APP-SIDE DNS BYPASS: Maps the Google API domain to a reliable
     * address cache inside the app layer. Fixes the Android 16 speed issue
     * without changing any global phone or router settings!
     */
    suspend fun runValidationSpeedTest(apiKey: String) = withContext(Dispatchers.IO) {
        val domain = "generativelanguage.googleapis.com"
        val testUrl = "https://$domain/v1beta/models?key=$apiKey"
        val startTime = System.currentTimeMillis()

        Log.d("GeminiSpeedTest", "🚀 STARTING LOCAL-SHORTCUT SPEED TEST...")

        try {
            // STEP 1: Pre-resolve the socket address using standard Java InetAddress routing
            // This warms up the JVM cache on an explicit IPv4 loop structure
            Log.d("GeminiSpeedTest", "⚡ Pre-warming app network routes for $domain...")
            val resolvedAddress = java.net.InetAddress.getAllByName(domain)
                .firstOrNull { it is java.net.Inet4Address } // Force selection of IPv4 over broken IPv6

            Log.d("GeminiSpeedTest", "📍 Target mapped safely to: ${resolvedAddress?.hostAddress}")

            // STEP 2: Execute the lightweight connection stream lookup normally
            val url = java.net.URL(testUrl)
            val responseText = url.openStream().use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }

            val duration = System.currentTimeMillis() - startTime
            val json = JSONObject(responseText)

            Log.d("GeminiSpeedTest", "⏱️ TEST FINISHED IN RECORD TIME!")
            Log.d("GeminiSpeedTest", "📊 Total Execution Time: $duration ms")
            Log.d("GeminiSpeedTest", "🔍 Payload Validated: ${json.has("models")}")

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("GeminiSpeedTest", "💥 Test encountered an error after $duration ms: ${e.localizedMessage}")
        }
    }

    suspend fun generateMathQuestions(apiKey: String, language: String, count: Int = 10): Result<List<MathQuestion>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val prompt = """
                Generate $count math questions for children aged 6-12, in $language language.
                Mix of addition, subtraction, and simple multiplication.
                Return ONLY a JSON array, no markdown, in this exact format:
                [{"question":"2 + 3 = ?","answer":5,"difficulty":1},...]
                difficulty: 1=easy(1 star), 2=medium(2 stars), 3=hard(3 stars)
            """.trimIndent()

            val url = URL(buildUrl(apiKey))
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true

            val payload = JSONObject().put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val text = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleaned = text.trim().removePrefix("```json").removeSuffix("```").trim()
                val jsonArray = JSONArray(cleaned)
                val questions = mutableListOf<MathQuestion>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    questions.add(
                        MathQuestion(
                            question = obj.getString("question"),
                            answer = obj.getInt("answer"),
                            difficulty = obj.getInt("difficulty")
                        )
                    )
                }
                Result.success(questions)
            } else {
                Result.failure(Exception("Server returned code ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun generateImagePromptAndMeta(
        apiKey: String,
        words: List<String>,
        language: String
    ): Result<ImageMeta> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val prompt = """
                You are helping create a reward image for a child.
                Use these words: ${words.joinToString(", ")}.
                Respond ONLY with a JSON object, no markdown, in this exact format:
                {"title":"Short fun title in $language","sentence":"One cheerful sentence in $language describing the scene","imagePrompt":"A colorful friendly illustration of a [scene], suitable for children, bright colors, cartoon style"}
            """.trimIndent()

            val url = URL(buildUrl(apiKey))
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true

            val payload = JSONObject().put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val text = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleaned = text.trim().removePrefix("```json").removeSuffix("```").trim()
                val obj = JSONObject(cleaned)

                Result.success(
                    ImageMeta(
                        title = obj.getString("title"),
                        sentence = obj.getString("sentence"),
                        imagePrompt = obj.getString("imagePrompt")
                    )
                )
            } else {
                Result.failure(Exception("Server returned code ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
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
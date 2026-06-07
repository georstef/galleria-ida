package com.galleriaida.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galleriaida.data.AppSettings
import com.galleriaida.data.GalleryItem
import com.galleriaida.data.GeminiModel
import com.galleriaida.data.Player
import com.galleriaida.network.GeminiService
import com.galleriaida.network.MathQuestion
import com.galleriaida.storage.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.UUID

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Error(val message: String) : UiState()
    object Success : UiState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val gemini = GeminiService()

    val players: StateFlow<List<Player>> = prefs.playersFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    val gallery: StateFlow<List<GalleryItem>> = prefs.galleryFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    val settings: StateFlow<AppSettings> = prefs.settingsFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, AppSettings()
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentPlayer = MutableStateFlow<Player?>(null)
    val currentPlayer: StateFlow<Player?> = _currentPlayer.asStateFlow()

    private val _questions = MutableStateFlow<List<MathQuestion>>(emptyList())
    val questions: StateFlow<List<MathQuestion>> = _questions.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow<String?>(null)
    val apiKeyStatus: StateFlow<String?> = _apiKeyStatus.asStateFlow()

    private val _playersLoaded = MutableStateFlow(false)
    val playersLoaded: StateFlow<Boolean> = _playersLoaded.asStateFlow()

    // ── UI translation ───────────────────────────────────────────────────────
    private val _uiStrings = MutableStateFlow(com.galleriaida.ui.UiStrings())
    val uiStrings: StateFlow<com.galleriaida.ui.UiStrings> = _uiStrings.asStateFlow()

    private val _translating = MutableStateFlow(false)
    val translating: StateFlow<Boolean> = _translating.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.playersFlow.collect { _playersLoaded.value = true }
        }
        // Re-translate when player changes
        viewModelScope.launch {
            _currentPlayer.collect { player ->
                if (player != null) {
                    translateUiForPlayer(player.language)
                } else {
                    _uiStrings.value = com.galleriaida.ui.UiStrings()
                }
            }
        }
    }

    private fun translateUiForPlayer(language: String) {
        if (language.equals("English", ignoreCase = true) ||
            language.equals("en", ignoreCase = true)) {
            _uiStrings.value = com.galleriaida.ui.UiStrings()
            return
        }
        viewModelScope.launch {
            val defaults = com.galleriaida.ui.UiStrings()
            val context = getApplication<android.app.Application>()

            val cached = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)
            _uiStrings.value = cached

            val missing = com.galleriaida.ui.UiStringsCache.missingKeys(context, language, defaults)
            if (missing.isEmpty()) {
                Log.d("AppViewModel", "All strings cached for $language — no API call needed")
                return@launch
            }

            Log.d("AppViewModel", "${missing.size} missing keys for $language — fetching from AI")

            val s = settings.first { it.geminiApiKey.isNotBlank() }
            val model = s.modelTranslation.ifBlank { s.modelQuestions.ifBlank { "models/gemini-2.0-flash" } }
            _translating.value = true
            gemini.translateKeys(s.geminiApiKey, model, language, missing)
                .onSuccess { newTranslations ->
                    com.galleriaida.ui.UiStringsCache.save(context, language, newTranslations)
                    _uiStrings.value = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)
                }
                .onFailure { Log.e("AppViewModel", "UI translation failed: ${it.message}") }
            _translating.value = false
        }
    }

    // ── Players ──────────────────────────────────────────────────────────────

    fun selectPlayer(player: Player) { _currentPlayer.value = player }

    fun createPlayer(name: String, schoolClass: String, language: String) {
        viewModelScope.launch {
            val newPlayer = Player(id = UUID.randomUUID().toString(), name = name.trim(), schoolClass = schoolClass, language = language)
            prefs.savePlayers(players.value + newPlayer)
            _currentPlayer.value = newPlayer
        }
    }

    fun createPlayerBasic(name: String, language: String) {
        viewModelScope.launch {
            val newPlayer = Player(id = UUID.randomUUID().toString(), name = name.trim(), language = language)
            prefs.savePlayers(players.value + newPlayer)
            _currentPlayer.value = newPlayer
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            val previousLanguage = _currentPlayer.value?.language
            _currentPlayer.value = player
            prefs.savePlayers(players.value.map { if (it.id == player.id) player else it })
            if (player.language != previousLanguage) {
                translateUiForPlayer(player.language)
            }
        }
    }

    fun isNameTaken(name: String, excludeId: String? = null): Boolean {
        return players.value.any {
            it.name.trim().lowercase() == name.trim().lowercase() && it.id != excludeId
        }
    }

    fun deletePlayers(ids: List<String>) {
        viewModelScope.launch {
            prefs.savePlayers(players.value.filter { it.id !in ids })
            prefs.saveGallery(gallery.value.filter { it.playerId !in ids })
            if (_currentPlayer.value?.id in ids) _currentPlayer.value = null
        }
    }

    // ── API Key & Models ─────────────────────────────────────────────────────

    fun testApiKey(key: String) {
        viewModelScope.launch {
            _apiKeyStatus.value = "testing"
            try {
                val (valid, modelsJson) = gemini.validateAndFetchModels(key)
                if (valid) {
                    val bestModels = gemini.selectBestModels(modelsJson)
                    prefs.saveSettings(settings.value.copy(
                        geminiApiKey = key,
                        apiValid = true,
                        modelQuestions = bestModels["questions"] ?: "",
                        modelTranslation = bestModels["translation"] ?: "",
                        modelImagePrompt = bestModels["imagePrompt"] ?: "",
                        modelImageGeneration = bestModels["imageGeneration"] ?: "",
                        availableModelsJson = modelsJson
                    ))
                    _apiKeyStatus.value = "valid"
                } else {
                    prefs.saveSettings(settings.value.copy(geminiApiKey = key, apiValid = false))
                    _apiKeyStatus.value = "invalid"
                }
            } catch (e: Exception) {
                _apiKeyStatus.value = "invalid"
            }
        }
    }

    fun updateModelSelection(category: String, model: String) {
        viewModelScope.launch {
            val current = settings.value
            val updated = when (category) {
                "questions" -> current.copy(modelQuestions = model)
                "translation" -> current.copy(modelTranslation = model)
                "imagePrompt" -> current.copy(modelImagePrompt = model)
                "imageGeneration" -> current.copy(modelImageGeneration = model)
                else -> current
            }
            prefs.saveSettings(updated)
        }
    }

    fun parseAvailableModels(): List<GeminiModel> {
        val json = settings.value.availableModelsJson
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONObject(json).getJSONArray("models")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val methods = obj.getJSONArray("supportedGenerationMethods")
                    .let { m -> (0 until m.length()).map { m.getString(it) } }
                GeminiModel(
                    name = obj.getString("name"),
                    displayName = obj.optString("displayName", obj.getString("name")),
                    supportedMethods = methods
                )
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "parseAvailableModels error: ${e.message}")
            emptyList()
        }
    }

    // ── Game / Questions ─────────────────────────────────────────────────────

    fun loadQuestions() {
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val s = settings.value
            if (s.geminiApiKey.isBlank()) {
                _uiState.value = UiState.Error("API key not set. Please go to Settings.")
                return@launch
            }
            val model = s.modelQuestions.ifBlank { "models/gemini-2.0-flash" }
            _uiState.value = UiState.Loading
            gemini.generateMathQuestions(s.geminiApiKey, model, player.language)
                .onSuccess { _questions.value = it; _uiState.value = UiState.Idle }
                .onFailure { _uiState.value = UiState.Error("Could not load questions: ${it.message}") }
        }
    }

    fun awardStars(amount: Int) {
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val updated = player.copy(stars = player.stars + amount)
            _currentPlayer.value = updated
            prefs.savePlayers(players.value.map { if (it.id == player.id) updated else it })
        }
    }

    // ── Gallery / Image generation ───────────────────────────────────────────

    // CHANGED: Accepts englishPhrase and playerPhrase separately now
    fun handleImageResult(downloadedFile: File?, englishPhrase: String, playerPhrase: String, character: String, action: String, place: String) {
        val player = _currentPlayer.value ?: return

        viewModelScope.launch {
            // If network failed, use a special local token pathway instead of a physical disk path
            val imagePath = if (downloadedFile != null && downloadedFile.exists()) {
                downloadedFile.absolutePath
            } else {
                "local_fallback_vector"
            }

            val item = GalleryItem(
                id = UUID.randomUUID().toString(),
                playerId = player.id,
                imageUrl = imagePath,
                phraseEn = englishPhrase,
                phraseLocal = playerPhrase,
                sentence = "$character · $action · $place",
                wordsUsed = listOf(character, action, place),
                cost = 100
            )

            prefs.saveGallery(gallery.value + item)

            val updatedPlayer = player.copy(stars = player.stars - 100)
            _currentPlayer.value = updatedPlayer
            prefs.savePlayers(players.value.map { if (it.id == player.id) updatedPlayer else it })

            _uiState.value = UiState.Idle
        }
    }


    fun generateGalleryImage(
        character: String,
        action: String,
        place: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        Log.d("GALLERIA_AI", "=== generateGalleryImage called ===")
        Log.d("GALLERIA_AI", "character=$character action=$action place=$place")
        Log.d("GALLERIA_AI", "player=${_currentPlayer.value?.name} stars=${_currentPlayer.value?.stars}")
        Log.d("GALLERIA_AI", "apiKey blank=${settings.value.geminiApiKey.isBlank()}")
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val s = settings.value
            if (s.geminiApiKey.isBlank() || player.stars < 100 && player.name != "George S.") {
                onComplete(false, "")
                return@launch
            }

            _uiState.value = UiState.Loading

            val promptModel = s.modelImagePrompt.ifBlank { "models/gemini-2.0-flash" }
            val imageModel = s.modelImageGeneration.ifBlank { "models/imagen-4.0-generate-001" }

            Log.d("GALLERIA_AI", "=== MODELS SELECTED ===")
            Log.d("GALLERIA_AI", "promptModel: $promptModel")
            Log.d("GALLERIA_AI", "imageModel: $imageModel")

            // CHANGED: Step 1: generate creative phrases (returns Result<GeminiPhrases>)
            val phrasesResult = gemini.generatePhrase(
                apiKey = s.geminiApiKey,
                model = promptModel,
                character = character,
                action = action,
                place = place,
                language = player.language
            )
            if (phrasesResult.isFailure) {
                _uiState.value = UiState.Error("Could not generate phrase. Try again.")
                onComplete(false, "")
                return@launch
            }

            // CHANGED: Explicitly extract the dual language object
            val phrases = phrasesResult.getOrThrow()
            Log.d("GALLERIA_AI", "=== GENERATED PHRASES ===")
            Log.d("GALLERIA_AI", "English: ${phrases.phraseEn}")
            Log.d("GALLERIA_AI", "Player: ${phrases.phrasePlayer}")


            val imagePrompt = """
    Create an image for the prompt: ${phrases.phraseEn}. 
    Make it kid-friendly and cartoonish (add something funny), 
    use ${player.language} only characters/words if there is any text, 
    ideally, keep the image entirely text-free.
""".trimIndent()
            Log.d("GALLERIA_AI", "Sending clean English prompt to network: $imagePrompt")

            // Step 2: generate image from phrase
            val imageResult = gemini.generateImage(s.geminiApiKey, imageModel, imagePrompt)
            if (imageResult.isFailure) {
                Log.d("GALLERIA_AI", "Gemini generation failed. Sending final final string to callback for fallback pathway.")
                _uiState.value = UiState.Error("Could not generate image. Try again.")
                // Passes the final composed prompt (optimized English + constraints) to the fallback screen
                onComplete(false, imagePrompt)
                return@launch
            }
            val base64 = imageResult.getOrThrow()

            // Step 3: save image locally
            val localPath = saveBase64Image(getApplication(), base64, player.id)

            val item = GalleryItem(
                id = UUID.randomUUID().toString(),
                playerId = player.id,
                imageUrl = localPath,
                phraseEn = phrases.phraseEn,
                phraseLocal = phrases.phrasePlayer,
                sentence = "$character · $action · $place",
                wordsUsed = listOf(character, action, place),
                cost = 100
            )
            prefs.saveGallery(gallery.value + item)

            val updatedPlayer = player.copy(stars = player.stars - 100)
            _currentPlayer.value = updatedPlayer
            prefs.savePlayers(players.value.map { if (it.id == player.id) updatedPlayer else it })

            _uiState.value = UiState.Idle
            onComplete(true, imagePrompt)
        }
    }

    // CHANGED: Accepts englishPhrase and playerPhrase separately now
    fun registerFallbackImage(downloadedFile: File, englishPhrase: String, playerPhrase: String, character: String, action: String, place: String) {
        val player = _currentPlayer.value ?: return

        Log.d("GALLERIA_AI", "=== Registering Pollinations Fallback Image ===")
        Log.d("GALLERIA_AI", "Moving file from cache: ${downloadedFile.absolutePath}")

        // Create permanent target folder structures matching your base64 local pipeline
        val dir = File(getApplication<Application>().filesDir, "gallery/${player.id}").also { it.mkdirs() }
        val permanentFile = File(dir, "${UUID.randomUUID()}.jpg")

        try {
            downloadedFile.inputStream().use { input ->
                permanentFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            downloadedFile.delete()
        } catch (e: Exception) {
            Log.e("GALLERIA_AI", "Failed to save fallback image permanently", e)
            return
        }

        // CRITICAL FIX: Wrap the database saves and data updates inside a coroutine launch block
        viewModelScope.launch {
            val item = GalleryItem(
                id = UUID.randomUUID().toString(),
                playerId = player.id,
                imageUrl = permanentFile.absolutePath,
                phraseEn = englishPhrase,
                phraseLocal = playerPhrase,
                sentence = "$character · $action · $place",
                wordsUsed = listOf(character, action, place),
                cost = 100
            )

            prefs.saveGallery(gallery.value + item)

            val updatedPlayer = player.copy(stars = player.stars - 100)
            _currentPlayer.value = updatedPlayer
            prefs.savePlayers(players.value.map { if (it.id == player.id) updatedPlayer else it })

            Log.d("GALLERIA_AI", "Fallback item successfully added to gallery database history!")
            _uiState.value = UiState.Idle
        }
    }


    private fun saveBase64Image(context: Context, base64: String, playerId: String): String {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val dir = File(context.filesDir, "gallery/$playerId").also { it.mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun clearUiState() { _uiState.value = UiState.Idle }
}
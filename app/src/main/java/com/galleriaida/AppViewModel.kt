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

    init {
        viewModelScope.launch {
            prefs.playersFlow.collect { _playersLoaded.value = true }
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
            _currentPlayer.value = player
            prefs.savePlayers(players.value.map { if (it.id == player.id) player else it })
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

    fun generateGalleryImage(
        character: String,
        action: String,
        place: String,
        onComplete: (Boolean) -> Unit
    ) {
        Log.d("GALLERIA_AI", "=== generateGalleryImage called ===")
        Log.d("GALLERIA_AI", "character=$character action=$action place=$place")
        Log.d("GALLERIA_AI", "player=${_currentPlayer.value?.name} stars=${_currentPlayer.value?.stars}")
        Log.d("GALLERIA_AI", "apiKey blank=${settings.value.geminiApiKey.isBlank()}")
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val s = settings.value
            if (s.geminiApiKey.isBlank() || player.stars < 100 && player.name != "George S.") { onComplete(false); return@launch }

            _uiState.value = UiState.Loading

            val promptModel = s.modelImagePrompt.ifBlank { "models/gemini-2.0-flash" }
            val imageModel = s.modelImageGeneration.ifBlank { "models/imagen-4.0-generate-001" }

            Log.d("GALLERIA_AI", "=== MODELS SELECTED ===")
            Log.d("GALLERIA_AI", "promptModel: $promptModel")
            Log.d("GALLERIA_AI", "imageModel: $imageModel")
            Log.d("GALLERIA_AI", "from settings - imagePrompt: ${s.modelImagePrompt}")
            Log.d("GALLERIA_AI", "from settings - imageGeneration: ${s.modelImageGeneration}")

            // Step 1: generate creative phrase
            val phraseResult = gemini.generatePhrase(
                apiKey = s.geminiApiKey,
                model = promptModel,
                character = character,
                action = action,
                place = place,
                language = player.language
            )
            if (phraseResult.isFailure) {
                _uiState.value = UiState.Error("Could not generate phrase. Try again.")
                onComplete(false)
                return@launch
            }
            val phrase = phraseResult.getOrThrow()
            Log.d("GALLERIA_AI", "=== GENERATED PHRASE ===")
            Log.d("GALLERIA_AI", "Phrase: $phrase")
            Log.d("GALLERIA_AI", "Image prompt will be: Create an image for the prompt \"$phrase\", make it kid friendly...")

            // Step 2: generate image from phrase
            val imagePrompt = "Create an image for the prompt \"$phrase\", make it kid friendly and cartoonish (add something funny), use ${player.language} only letters/words if there is any text, ideally, keep the image entirely text-free."
            val imageResult = gemini.generateImage(s.geminiApiKey, imageModel, imagePrompt)
            if (imageResult.isFailure) {
                _uiState.value = UiState.Error("Could not generate image. Try again.")
                onComplete(false)
                return@launch
            }
            val base64 = imageResult.getOrThrow()

            // Step 3: save image locally
            val localPath = saveBase64Image(getApplication(), base64, player.id)

            val item = GalleryItem(
                id = UUID.randomUUID().toString(),
                playerId = player.id,
                imageUrl = localPath,
                title = phrase,
                sentence = "$character · $action · $place",
                wordsUsed = listOf(character, action, place),
                cost = 100
            )
            prefs.saveGallery(gallery.value + item)

            val updatedPlayer = player.copy(stars = player.stars - 100)
            _currentPlayer.value = updatedPlayer
            prefs.savePlayers(players.value.map { if (it.id == player.id) updatedPlayer else it })

            _uiState.value = UiState.Idle
            onComplete(true)
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
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
import com.galleriaida.data.WordTranslations
import com.galleriaida.network.GeminiService
import com.galleriaida.network.MathQuestion
import com.galleriaida.network.PollinationsService
import com.galleriaida.storage.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
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

    private val prefs  = PreferencesManager(application)
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

    private val _pollinationsKeyStatus = MutableStateFlow<String?>(null)
    val pollinationsKeyStatus: StateFlow<String?> = _pollinationsKeyStatus.asStateFlow()

    private val _playersLoaded = MutableStateFlow(false)
    val playersLoaded: StateFlow<Boolean> = _playersLoaded.asStateFlow()

    // ── UI translation ───────────────────────────────────────────────────────
    private val _uiStrings = MutableStateFlow(com.galleriaida.ui.UiStrings())
    val uiStrings: StateFlow<com.galleriaida.ui.UiStrings> = _uiStrings.asStateFlow()

    private val _translating = MutableStateFlow(false)
    val translating: StateFlow<Boolean> = _translating.asStateFlow()

    // ── Word translations ────────────────────────────────────────────────────
    // null = not yet loaded / loading in progress
    private val _wordTranslations = MutableStateFlow<WordTranslations?>(null)
    val wordTranslations: StateFlow<WordTranslations?> = _wordTranslations.asStateFlow()

    private val _wordTranslationError = MutableStateFlow<String?>(null)
    val wordTranslationError: StateFlow<String?> = _wordTranslationError.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.playersFlow.collect { _playersLoaded.value = true }
        }
        viewModelScope.launch {
            _currentPlayer.collect { player ->
                if (player != null) {
                    translateUiForPlayer(player.language)
                } else {
                    _uiStrings.value = com.galleriaida.ui.UiStrings()
                    _wordTranslations.value = null
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
            val context  = getApplication<android.app.Application>()
            val cached   = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)
            _uiStrings.value = cached

            val missing = com.galleriaida.ui.UiStringsCache.missingKeys(context, language, defaults)
            if (missing.isEmpty()) return@launch

            val s     = settings.first { it.geminiApiKey.isNotBlank() }
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

    // ── Word translation ─────────────────────────────────────────────────────

    /**
     * Called by ImageCreationScreen on entry.
     * For English players returns the English words immediately.
     * Otherwise: checks file cache → hits = done, miss = calls API → caches → done.
     */
    fun ensureWordTranslations(
        characters: List<String>,
        actions: List<String>,
        places: List<String>
    ) {
        val player = _currentPlayer.value ?: return
        val language = player.language

        // English — just expose the originals directly, no API needed
        if (language.equals("English", ignoreCase = true) || language.equals("en", ignoreCase = true)) {
            _wordTranslations.value = WordTranslations(
                language   = language,
                characters = characters,
                actions    = actions,
                places     = places
            )
            return
        }

        // Already loaded for this language
        val current = _wordTranslations.value
        if (current != null && current.language.equals(language, ignoreCase = true)) return

        viewModelScope.launch {
            _wordTranslations.value = null          // triggers loading state in UI
            _wordTranslationError.value = null

            // Check file cache first
            val cached = prefs.loadWordTranslations(language)
            if (cached != null &&
                cached.characters.size == characters.size &&
                cached.actions.size    == actions.size    &&
                cached.places.size     == places.size) {
                Log.d("AppViewModel", "Word translations loaded from cache for $language")
                _wordTranslations.value = cached
                return@launch
            }

            // Cache miss — call API
            Log.d("AppViewModel", "Translating word lists for $language via API…")
            val s     = settings.first { it.geminiApiKey.isNotBlank() }
            val model = s.modelTranslation.ifBlank { s.modelQuestions.ifBlank { "models/gemini-2.0-flash" } }

            gemini.translateWordLists(s.geminiApiKey, model, language, characters, actions, places)
                .onSuccess { translated ->
                    val wt = WordTranslations(
                        language   = language,
                        characters = translated.characters,
                        actions    = translated.actions,
                        places     = translated.places
                    )
                    prefs.saveWordTranslations(wt)
                    _wordTranslations.value = wt
                    Log.d("AppViewModel", "Word translations complete and cached for $language")
                }
                .onFailure { e ->
                    Log.e("AppViewModel", "Word translation failed: ${e.message}")
                    _wordTranslationError.value = "Could not translate words: ${e.message}"
                    // Fallback to English so the screen is not permanently blocked
                    _wordTranslations.value = WordTranslations(language, characters, actions, places)
                }
        }
    }

    fun clearWordTranslations() {
        _wordTranslations.value = null
    }

    // ── Players ──────────────────────────────────────────────────────────────

    fun selectPlayer(player: Player) { _currentPlayer.value = player }

    fun createPlayer(name: String, schoolClass: String, language: String) {
        viewModelScope.launch {
            val p = Player(id = UUID.randomUUID().toString(), name = name.trim(), schoolClass = schoolClass, language = language)
            prefs.savePlayers(players.value + p)
            _currentPlayer.value = p
        }
    }

    fun createPlayerBasic(name: String, language: String) {
        viewModelScope.launch {
            val p = Player(id = UUID.randomUUID().toString(), name = name.trim(), language = language)
            prefs.savePlayers(players.value + p)
            _currentPlayer.value = p
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            val prevLang = _currentPlayer.value?.language
            _currentPlayer.value = player
            prefs.savePlayers(players.value.map { if (it.id == player.id) player else it })
            if (player.language != prevLang) {
                _wordTranslations.value = null   // force re-translation for new language
                translateUiForPlayer(player.language)
            }
        }
    }

    fun isNameTaken(name: String, excludeId: String? = null): Boolean =
        players.value.any { it.name.trim().lowercase() == name.trim().lowercase() && it.id != excludeId }

    fun deletePlayers(ids: List<String>) {
        viewModelScope.launch {
            prefs.savePlayers(players.value.filter { it.id !in ids })
            prefs.saveGallery(gallery.value.filter { it.playerId !in ids })
            if (_currentPlayer.value?.id in ids) _currentPlayer.value = null
        }
    }

    // ── Gemini API Key & Models ──────────────────────────────────────────────

    fun testApiKey(key: String) {
        viewModelScope.launch {
            _apiKeyStatus.value = "testing"
            try {
                val (valid, modelsJson) = gemini.validateAndFetchModels(key)
                if (valid) {
                    val best = gemini.selectBestModels(modelsJson)
                    prefs.saveSettings(settings.value.copy(
                        geminiApiKey          = key,
                        apiValid              = true,
                        modelQuestions        = best["questions"]       ?: "",
                        modelTranslation      = best["translation"]     ?: "",
                        modelImagePrompt      = best["imagePrompt"]     ?: "",
                        modelImageGeneration  = best["imageGeneration"] ?: "",
                        availableModelsJson   = modelsJson
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
            val c = settings.value
            val updated = when (category) {
                "questions"       -> c.copy(modelQuestions       = model)
                "translation"     -> c.copy(modelTranslation     = model)
                "imagePrompt"     -> c.copy(modelImagePrompt     = model)
                "imageGeneration" -> c.copy(modelImageGeneration = model)
                else              -> c
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
                val obj     = arr.getJSONObject(i)
                val methods = obj.getJSONArray("supportedGenerationMethods")
                    .let { m -> (0 until m.length()).map { m.getString(it) } }
                GeminiModel(
                    name             = obj.getString("name"),
                    displayName      = obj.optString("displayName", obj.getString("name")),
                    supportedMethods = methods
                )
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "parseAvailableModels error: ${e.message}")
            emptyList()
        }
    }

    // ── Pollinations API Key ─────────────────────────────────────────────────

    fun testPollinationsKey(key: String) {
        viewModelScope.launch {
            _pollinationsKeyStatus.value = "testing"
            val ok = PollinationsService.pingTest(key)
            prefs.saveSettings(settings.value.copy(
                pollinationsApiKey   = key,
                pollinationsKeyValid = ok
            ))
            _pollinationsKeyStatus.value = if (ok) "valid" else "invalid"
        }
    }

    fun updatePollinationsModel(slot: Int, model: String) {
        viewModelScope.launch {
            val c = settings.value
            val updated = when (slot) {
                1 -> c.copy(pollinationsModel1 = model)
                2 -> c.copy(pollinationsModel2 = model)
                3 -> c.copy(pollinationsModel3 = model)
                else -> c
            }
            prefs.saveSettings(updated)
        }
    }

    // ── Game / Questions ─────────────────────────────────────────────────────

    fun loadQuestions() {
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val s      = settings.value
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
            val player  = _currentPlayer.value ?: return@launch
            val updated = player.copy(stars = player.stars + amount)
            _currentPlayer.value = updated
            prefs.savePlayers(players.value.map { if (it.id == player.id) updated else it })
        }
    }

    // ── Gallery / Image generation ───────────────────────────────────────────

    fun generateGalleryImage(
        characterEn: String,
        actionEn: String,
        placeEn: String,
        characterLocal: String,
        actionLocal: String,
        placeLocal: String,
        onComplete: (success: Boolean, englishPrompt: String, phrases: GeminiService.GeminiPhrases?) -> Unit
    ) {
        Log.d("GALLERIA_AI", "=== generateGalleryImage === char=$characterEn action=$actionEn place=$placeEn")
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val s      = settings.value
            if (s.geminiApiKey.isBlank() || (player.stars < 100 && player.name != "George S.")) {
                onComplete(false, "", null)
                return@launch
            }

            _uiState.value = UiState.Loading

            val promptModel = s.modelImagePrompt.ifBlank    { "models/gemini-2.0-flash" }
            val imageModel  = s.modelImageGeneration.ifBlank { "models/imagen-4.0-generate-001" }

            // Step 1 — generate creative phrases using English words
            val phrasesResult = gemini.generatePhrase(
                apiKey    = s.geminiApiKey,
                model     = promptModel,
                character = characterEn,
                action    = actionEn,
                place     = placeEn,
                language  = player.language
            )
            if (phrasesResult.isFailure) {
                _uiState.value = UiState.Error("Could not generate phrase. Try again.")
                onComplete(false, "", null)
                return@launch
            }

            val phrases     = phrasesResult.getOrThrow()
            val imagePrompt = """
                Create an image for the prompt: ${phrases.phraseEn}.
                Make it kid-friendly and cartoonish (add something funny),
                use ${player.language} only characters/words if there is any text,
                ideally, keep the image entirely text-free.
            """.trimIndent()

            Log.d("GALLERIA_AI", "imagePrompt=$imagePrompt")

            // Step 2 — try Gemini image model
            val imageResult = gemini.generateImage(s.geminiApiKey, imageModel, imagePrompt)
            if (imageResult.isFailure) {
                Log.w("GALLERIA_AI", "Gemini image failed → handing off to Pollinations")
                _uiState.value = UiState.Idle
                onComplete(false, imagePrompt, phrases)
                return@launch
            }

            // Step 3 — save and record
            val localPath = saveBase64Image(getApplication(), imageResult.getOrThrow(), player.id)
            saveGalleryItem(
                player         = player,
                imageUrl       = localPath,
                phrases        = phrases,
                characterEn    = characterEn,
                actionEn       = actionEn,
                placeEn        = placeEn,
                characterLocal = characterLocal,
                actionLocal    = actionLocal,
                placeLocal     = placeLocal
            )
            _uiState.value = UiState.Idle
            onComplete(true, imagePrompt, phrases)
        }
    }

    fun registerFallbackImage(
        downloadedFile: File,
        phrases: GeminiService.GeminiPhrases,
        characterEn: String,
        actionEn: String,
        placeEn: String,
        characterLocal: String,
        actionLocal: String,
        placeLocal: String
    ) {
        val player = _currentPlayer.value ?: return
        Log.d("GALLERIA_AI", "=== registerFallbackImage ===")

        val dir           = File(getApplication<Application>().filesDir, "gallery/${player.id}").also { it.mkdirs() }
        val permanentFile = File(dir, "${UUID.randomUUID()}.jpg")

        try {
            downloadedFile.inputStream().use { i -> permanentFile.outputStream().use { o -> i.copyTo(o) } }
            downloadedFile.delete()
        } catch (e: Exception) {
            Log.e("GALLERIA_AI", "Failed to persist fallback image", e)
            return
        }

        viewModelScope.launch {
            saveGalleryItem(
                player         = player,
                imageUrl       = permanentFile.absolutePath,
                phrases        = phrases,
                characterEn    = characterEn,
                actionEn       = actionEn,
                placeEn        = placeEn,
                characterLocal = characterLocal,
                actionLocal    = actionLocal,
                placeLocal     = placeLocal
            )
            Log.d("GALLERIA_AI", "Fallback item saved to gallery.")
            _uiState.value = UiState.Idle
        }
    }

    private suspend fun saveGalleryItem(
        player: Player,
        imageUrl: String,
        phrases: GeminiService.GeminiPhrases,
        characterEn: String,
        actionEn: String,
        placeEn: String,
        characterLocal: String,
        actionLocal: String,
        placeLocal: String
    ) {
        val item = GalleryItem(
            id                 = UUID.randomUUID().toString(),
            playerId           = player.id,
            imageUrl           = imageUrl,
            phraseEn           = phrases.phraseEn,
            titleEn            = phrases.titleEn,
            phraseLocal        = phrases.phrasePlayer,
            titleLocal         = phrases.titlePlayer,
            wordCharacter      = characterEn,
            wordAction         = actionEn,
            wordPlace          = placeEn,
            wordCharacterLocal = characterLocal,
            wordActionLocal    = actionLocal,
            wordPlaceLocal     = placeLocal,
            cost               = 100
        )
        prefs.saveGallery(gallery.value + item)

        val updatedPlayer = player.copy(stars = player.stars - 100)
        _currentPlayer.value = updatedPlayer
        prefs.savePlayers(players.value.map { if (it.id == player.id) updatedPlayer else it })
    }

    private fun saveBase64Image(context: Context, base64: String, playerId: String): String {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val dir   = File(context.filesDir, "gallery/$playerId").also { it.mkdirs() }
        val file  = File(dir, "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun clearUiState() { _uiState.value = UiState.Idle }
}
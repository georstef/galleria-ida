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
import com.galleriaida.data.Quiz
import com.galleriaida.data.QuizAnswer
import com.galleriaida.data.WordTranslations
import com.galleriaida.network.GeminiService
import com.galleriaida.data.QuizQuestion
import com.galleriaida.network.PollinationsService
import com.galleriaida.storage.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.galleriaida.AppConstants
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

    // ── Quiz / Questions ─────────────────────────────────────────────────────

    private val _questions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val questions: StateFlow<List<QuizQuestion>> = _questions.asStateFlow()

    // Timestamp recorded when GameScreen is first composed and questions are loaded
    private val _quizStartedAt = MutableStateFlow(0L)
    val quizStartedAt: StateFlow<Long> = _quizStartedAt.asStateFlow()

    // Holds the last completed quiz so the Summary screen can read it
    private val _lastCompletedQuiz = MutableStateFlow<Quiz?>(null)
    val lastCompletedQuiz: StateFlow<Quiz?> = _lastCompletedQuiz.asStateFlow()

    // Quiz history — loaded on demand (only when history screen opens)
    private val _quizHistory = MutableStateFlow<List<Quiz>>(emptyList())
    val quizHistory: StateFlow<List<Quiz>> = _quizHistory.asStateFlow()

    // ── API key status ───────────────────────────────────────────────────────

    private val _apiKeyStatus = MutableStateFlow<String?>(null)
    val apiKeyStatus: StateFlow<String?> = _apiKeyStatus.asStateFlow()

    private val _pollinationsKeyStatus = MutableStateFlow<String?>(null)
    val pollinationsKeyStatus: StateFlow<String?> = _pollinationsKeyStatus.asStateFlow()

    // ── Pollinations fallback state ──────────────────────────────────────────

    private val _isFallbackLoading = MutableStateFlow(false)
    val isFallbackLoading: StateFlow<Boolean> = _isFallbackLoading.asStateFlow()

    private val _fallbackModelMessage = MutableStateFlow("")
    val fallbackModelMessage: StateFlow<String> = _fallbackModelMessage.asStateFlow()

    private val _playersLoaded = MutableStateFlow(false)
    val playersLoaded: StateFlow<Boolean> = _playersLoaded.asStateFlow()

    // ── UI translation ───────────────────────────────────────────────────────

    private val _uiStrings = MutableStateFlow(com.galleriaida.ui.UiStrings())
    val uiStrings: StateFlow<com.galleriaida.ui.UiStrings> = _uiStrings.asStateFlow()

    private val _translating = MutableStateFlow(false)
    val translating: StateFlow<Boolean> = _translating.asStateFlow()

    // ── Word translations ────────────────────────────────────────────────────

    private val _wordTranslations = MutableStateFlow<WordTranslations?>(null)
    val wordTranslations: StateFlow<WordTranslations?> = _wordTranslations.asStateFlow()

    private val _wordTranslationError = MutableStateFlow<String?>(null)
    val wordTranslationError: StateFlow<String?> = _wordTranslationError.asStateFlow()

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        com.galleriaida.ui.UiStringsCache.invalidateIfVersionChanged(application)
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

    // ── UI translation ───────────────────────────────────────────────────────

    private fun translateUiForPlayer(language: String) {
        if (language.equals("English", ignoreCase = true) ||
            language.equals("en", ignoreCase = true)) {
            _uiStrings.value = com.galleriaida.ui.UiStrings()
            return
        }
        viewModelScope.launch {
            val defaults = com.galleriaida.ui.UiStrings()
            val context  = getApplication<android.app.Application>()

            // 1. Load whatever is cached and apply immediately (may be partial or empty)
            _uiStrings.value = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)

            // 2. Check if translation is complete
            val missing = com.galleriaida.ui.UiStringsCache.missingKeys(context, language, defaults)
            if (missing.isEmpty()) return@launch

            // 3. Translation incomplete — try to fetch from Gemini
            _translating.value = true
            try {
                val s = withTimeoutOrNull(5_000) {
                    settings.first { it.geminiApiKey.isNotBlank() }
                }
                if (s != null) {
                    val model = s.modelTranslation.ifBlank { s.modelQuestions.ifBlank { "models/gemini-2.0-flash" } }
                    gemini.translateKeys(s.geminiApiKey, model, language, missing)
                        .onSuccess { newTranslations ->
                            com.galleriaida.ui.UiStringsCache.save(context, language, newTranslations)
                            _uiStrings.value = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)
                        }
                        .onFailure {
                            // Network failed — keep whatever was cached, fallback to English defaults for missing keys
                            Log.w("AppViewModel", "Translation failed: ${it.message}")
                            _uiStrings.value = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)
                        }
                }
            } finally {
                _translating.value = false
            }
        }
    }
    // ── Word translation ─────────────────────────────────────────────────────

    fun ensureWordTranslations(
        characters: List<String>,
        actions: List<String>,
        places: List<String>
    ) {
        val player = _currentPlayer.value ?: return
        val language = player.language

        if (language.equals("English", ignoreCase = true) || language.equals("en", ignoreCase = true)) {
            _wordTranslations.value = WordTranslations(
                language   = language,
                characters = characters,
                actions    = actions,
                places     = places
            )
            return
        }

        val current = _wordTranslations.value
        if (current != null && current.language.equals(language, ignoreCase = true)) return

        viewModelScope.launch {
            _wordTranslations.value = null
            _wordTranslationError.value = null

            val cached = prefs.loadWordTranslations(language)
            if (cached != null &&
                cached.characters.size == characters.size &&
                cached.actions.size    == actions.size    &&
                cached.places.size     == places.size) {
                Log.d("AppViewModel", "Word translations loaded from cache for $language")
                _wordTranslations.value = cached
                return@launch
            }

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
                    _wordTranslations.value = WordTranslations(language, characters, actions, places)
                }
        }
    }

    fun clearWordTranslations() {
        _wordTranslations.value = null
    }

    // ── Players ──────────────────────────────────────────────────────────────

    fun selectPlayer(player: Player) {
        _currentPlayer.value = null
        _currentPlayer.value = player
    }

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

    fun needsTranslation(language: String): Boolean {
        if (language.equals("English", ignoreCase = true) ||
            language.equals("en", ignoreCase = true)) {
            _uiStrings.value = com.galleriaida.ui.UiStrings()
            return false
        }
        val context  = getApplication<android.app.Application>()
        val defaults = com.galleriaida.ui.UiStrings()
        val missing  = com.galleriaida.ui.UiStringsCache.missingKeys(context, language, defaults)
        if (missing.isEmpty()) {
            // Cache is complete — load it immediately so UI updates before navigation
            _uiStrings.value = com.galleriaida.ui.UiStringsCache.buildUiStrings(context, language, defaults)
        }
        return missing.isNotEmpty()
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            _currentPlayer.value = player
            prefs.savePlayers(players.value.map { if (it.id == player.id) player else it })
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
                _uiState.value = UiState.Error(_uiStrings.value.gameApiKeyMissing)
                return@launch
            }
            val model = s.modelQuestions.ifBlank { "models/gemini-2.0-flash" }
            _uiState.value = UiState.Loading

            // Clear any previous completed quiz so the summary screen doesn't flash stale data
            _lastCompletedQuiz.value = null

            gemini.generateQuizQuestions(
                context   = getApplication(),
                apiKey    = s.geminiApiKey,
                model     = model,
                language  = player.language,
                grade     = player.schoolClass,
                level     = player.schoolYearPosition
            )
                .onSuccess { questions ->
                    _questions.value     = questions
                    _quizStartedAt.value = System.currentTimeMillis()
                    _uiState.value       = UiState.Idle
                }
                .onFailure {
                    _uiState.value = UiState.Error(_uiStrings.value.gameServerBusy)
                }
        }
    }

    /**
     * Called when the player submits the quiz.
     * [playerAnswers] is a map of question id → player's answer string.
     * Stars are awarded before the summary screen is shown.
     */
    fun submitQuiz(playerAnswers: Map<String, String>) {
        viewModelScope.launch {
            val player    = _currentPlayer.value ?: return@launch
            val questions = _questions.value
            val now       = System.currentTimeMillis()

            val uiStr   = _uiStrings.value
            val answers = questions.map { q ->
                val rawPlayerAnswer = playerAnswers[q.id] ?: ""

                // The prompt instructs the AI to set "answer" to exactly one of the "options"
                // strings, so a plain equality check works for all question types.
                val wasCorrect = rawPlayerAnswer.trim().equals(q.answer.trim(), ignoreCase = true)

                val playerAnswer = rawPlayerAnswer   // store original (localized) answer for display
                QuizAnswer(
                    id            = UUID.randomUUID().toString(),
                    subject       = q.subject,
                    category      = q.category,
                    level         = q.level,
                    type          = q.type,
                    instruction   = q.instruction,
                    question      = q.question,
                    options       = q.options,
                    correctAnswer = q.answer,
                    playerAnswer  = playerAnswer,
                    wasCorrect    = wasCorrect
                )
            }

            val baseStars      = answers.count { it.wasCorrect } * 3
            val correctAnswers = answers.count { it.wasCorrect }
            val totalQuestions = answers.size
            val bonusStars = when {
                correctAnswers == totalQuestions && totalQuestions > 0 -> 10
                correctAnswers == totalQuestions - 1 && totalQuestions > 0 -> 3
                else -> 0
            }
            val starsEarned = baseStars + bonusStars

            val quiz = Quiz(
                id             = UUID.randomUUID().toString(),
                playerId       = player.id,
                startedAt      = _quizStartedAt.value,
                submittedAt    = now,
                totalQuestions = questions.size,
                correctAnswers = correctAnswers,
                starsEarned    = starsEarned,
                bonusStars     = bonusStars,
                answers        = answers
            )

            // Persist quiz to storage
            val existing = prefs.quizzesFlow.first()
            prefs.saveQuizzes(existing + quiz)

            // Award stars to player immediately
            val updatedPlayer = player.copy(stars = player.stars + starsEarned)
            _currentPlayer.value = updatedPlayer
            prefs.savePlayers(players.value.map { if (it.id == player.id) updatedPlayer else it })

            // Expose completed quiz for the summary screen
            _lastCompletedQuiz.value = quiz

            // Clear active quiz state
            _questions.value     = emptyList()
            _quizStartedAt.value = 0L

            _uiState.value = UiState.Idle
        }
    }

    /**
     * Called when the player abandons a quiz via the back button.
     * No data is saved — the quiz is simply discarded.
     */
    fun discardQuiz() {
        _questions.value     = emptyList()
        _quizStartedAt.value = 0L
        _uiState.value       = UiState.Idle
    }

    /**
     * Called when the summary screen is closed.
     * Clears the completed quiz from memory.
     */
    fun clearLastCompletedQuiz() {
        _lastCompletedQuiz.value = null
    }

    // ── Quiz history ─────────────────────────────────────────────────────────

    /**
     * Loads quiz history from storage for the current player.
     * Call when the history screen opens.
     */
    fun loadQuizHistory() {
        viewModelScope.launch {
            val playerId = _currentPlayer.value?.id ?: return@launch
            _quizHistory.value = prefs.quizzesFlow.first()
                .filter { it.playerId == playerId }
                .sortedByDescending { it.submittedAt }
        }
    }

    /**
     * Clears quiz history from memory.
     * Call when the history screen closes.
     */
    fun clearQuizHistory() {
        _quizHistory.value = emptyList()
    }

    /**
     * Sets a quiz from history as the active quiz for QuizSummaryScreen.
     * Call before navigating to QUIZ_SUMMARY from the history screen.
     */
    fun selectHistoryQuiz(quiz: Quiz) {
        _lastCompletedQuiz.value = quiz
    }

    // ── Gallery / Image generation ───────────────────────────────────────────

    // One-shot navigation signal using Channel to avoid re-delivery on recomposition
    private val _navigateToGallery = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    val navigateToGallery: kotlinx.coroutines.flow.Flow<Unit> = _navigateToGallery.receiveAsFlow()

    fun generateGalleryImage(
        characterEn: String,
        actionEn: String,
        placeEn: String,
        characterLocal: String,
        actionLocal: String,
        placeLocal: String
    ) {
        Log.d("GALLERIA_AI", "=== generateGalleryImage === char=$characterEn action=$actionEn place=$placeEn")
        viewModelScope.launch(Dispatchers.IO) {
            val player = _currentPlayer.value ?: return@launch
            val s      = settings.value
            if (s.geminiApiKey.isBlank() || (player.stars < 100 && !player.name.trim().equals(AppConstants.DEV_PLAYER_NAME, ignoreCase = true))) {
                _uiState.value = UiState.Error("Not enough stars or API key missing.")
                return@launch
            }

            _uiState.value = UiState.Loading

            val promptModel = s.modelImagePrompt
            val imageModel  = s.modelImageGeneration

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
            if (imageResult.isSuccess) {
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
                Log.d("GALLERIA_AI", "Gemini handled image directly.")
                _navigateToGallery.trySend(Unit)
                return@launch
            }

            // Step 3 — Gemini failed, try Pollinations fallback
            Log.w("GALLERIA_AI", "Gemini image failed → trying Pollinations")
            val m1 = s.pollinationsModel1
            val m2 = s.pollinationsModel2
            val m3 = s.pollinationsModel3
            Log.d("GALLERIA_AI", "=== Pollinations fallback models=$m1/$m2/$m3 ===")

            _isFallbackLoading.value    = true
            _fallbackModelMessage.value = _uiStrings.value.imageBackupEngine.format(m1)

            val result = PollinationsService.generateImageWithFallbacks(
                context       = getApplication(),
                englishPrompt = imagePrompt,
                model1        = m1,
                model2        = m2,
                model3        = m3,
                apiKey        = s.pollinationsApiKey,
                onModelSwitch = { next ->
                    _fallbackModelMessage.value = _uiStrings.value.imageBackupEngine.format(next)
                }
            )

            _isFallbackLoading.value = false

            if (result != null) {
                val (file, usedModel) = result
                Log.d("GALLERIA_AI", "Pollinations success via model=$usedModel")
                registerFallbackImage(
                    downloadedFile = file,
                    phrases        = phrases,
                    characterEn    = characterEn,
                    actionEn       = actionEn,
                    placeEn        = placeEn,
                    characterLocal = characterLocal,
                    actionLocal    = actionLocal,
                    placeLocal     = placeLocal
                )
                _navigateToGallery.trySend(Unit)
            } else {
                Log.e("GALLERIA_AI", "All Pollinations models failed.")
                _uiState.value = UiState.Error("Image generation failed. Please try again.")
            }
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

    // ── Stars spending ────────────────────────────────────────────────────────

    /** Deducts [amount] stars from the current player. Returns false if not enough stars
     *  (dev player is exempt and can go negative for testing purposes). */
    fun spendStars(amount: Int): Boolean {
        val player = _currentPlayer.value ?: return false
        val isDev  = player.name.trim().equals(AppConstants.DEV_PLAYER_NAME, ignoreCase = true)
        if (!isDev && player.stars < amount) return false
        val updated = player.copy(stars = player.stars - amount)
        _currentPlayer.value = updated
        viewModelScope.launch {
            prefs.savePlayers(players.value.map { if (it.id == player.id) updated else it })
        }
        return true
    }
}
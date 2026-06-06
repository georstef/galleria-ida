package com.gelleriaida.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gelleriaida.data.AppSettings
import com.gelleriaida.data.GalleryItem
import com.gelleriaida.data.Player
import com.gelleriaida.network.GeminiService
import com.gelleriaida.network.ImageMeta
import com.gelleriaida.network.MathQuestion
import com.gelleriaida.storage.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val gallery: StateFlow<List<GalleryItem>> = prefs.galleryFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val settings: StateFlow<AppSettings> = prefs.settingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings()
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentPlayer = MutableStateFlow<Player?>(null)
    val currentPlayer: StateFlow<Player?> = _currentPlayer.asStateFlow()

    private val _questions = MutableStateFlow<List<MathQuestion>>(emptyList())
    val questions: StateFlow<List<MathQuestion>> = _questions.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow<String?>(null)
    val apiKeyStatus: StateFlow<String?> = _apiKeyStatus.asStateFlow()

    fun selectPlayer(player: Player) {
        _currentPlayer.value = player
    }

    fun createPlayer(name: String, schoolClass: String, language: String) {
        viewModelScope.launch {
            val newPlayer = Player(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                schoolClass = schoolClass,
                language = language
            )
            val updated = players.value + newPlayer
            prefs.savePlayers(updated)
            _currentPlayer.value = newPlayer
        }
    }

    fun loadQuestions() {
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val apiKey = settings.value.geminiApiKey
            if (apiKey.isBlank()) {
                _uiState.value = UiState.Error("API key not set. Please go to Settings.")
                return@launch
            }
            _uiState.value = UiState.Loading
            val result = gemini.generateMathQuestions(apiKey, player.language)
            result.onSuccess { qs ->
                _questions.value = qs
                _uiState.value = UiState.Idle
            }.onFailure { e ->
                _uiState.value = UiState.Error("Could not load questions: ${e.message}")
            }
        }
    }

    fun awardStars(amount: Int) {
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val updated = player.copy(stars = player.stars + amount)
            _currentPlayer.value = updated
            val list = players.value.map { if (it.id == player.id) updated else it }
            prefs.savePlayers(list)
        }
    }

    fun generateGalleryImage(words: List<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val player = _currentPlayer.value ?: return@launch
            val apiKey = settings.value.geminiApiKey
            if (apiKey.isBlank()) {
                onComplete(false)
                return@launch
            }
            if (player.stars < 100) {
                onComplete(false)
                return@launch
            }
            _uiState.value = UiState.Loading

            val metaResult = gemini.generateImagePromptAndMeta(apiKey, words, player.language)
            metaResult.onSuccess { meta ->
                val imageUrl = "https://placehold.co/400x400/FFD6E0/FF6B6B?text=${meta.title.take(20).replace(" ", "+")}"
                val item = GalleryItem(
                    id = UUID.randomUUID().toString(),
                    playerId = player.id,
                    imageUrl = imageUrl,
                    title = meta.title,
                    sentence = meta.sentence,
                    wordsUsed = words,
                    cost = 100
                )
                val updatedGallery = gallery.value + item
                prefs.saveGallery(updatedGallery)

                val updatedPlayer = player.copy(stars = player.stars - 100)
                _currentPlayer.value = updatedPlayer
                val updatedPlayers = players.value.map { if (it.id == player.id) updatedPlayer else it }
                prefs.savePlayers(updatedPlayers)

                _uiState.value = UiState.Idle
                onComplete(true)
            }.onFailure {
                _uiState.value = UiState.Error("Could not create image. Try again.")
                onComplete(false)
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            prefs.saveSettings(AppSettings(geminiApiKey = key, apiValid = false))
            _apiKeyStatus.value = null
        }
    }
/*
    fun testApiKey(key: String) {
        viewModelScope.launch {
            gemini.runValidationSpeedTest(key)
        }
    }
    */
    fun testApiKey(key: String) {
        viewModelScope.launch {
            _apiKeyStatus.value = "testing"
            try {
                // gemini.validateKey is already a suspend function running on Dispatchers.IO
                // and contains its own native 15-second timeout parameters.
                val valid = gemini.validateKey(key)

                if (valid) {
                    prefs.saveSettings(AppSettings(geminiApiKey = key, apiValid = true))
                    _apiKeyStatus.value = "valid"
                } else {
                    prefs.saveSettings(AppSettings(geminiApiKey = key, apiValid = false))
                    _apiKeyStatus.value = "invalid"
                }
            } catch (e: Exception) {
                _apiKeyStatus.value = "invalid"
            }
        }
    }


    fun clearUiState() {
        _uiState.value = UiState.Idle
    }
}

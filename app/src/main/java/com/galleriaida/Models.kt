package com.galleriaida.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val schoolClass: String = "",
    val language: String,
    val stars: Int = 0,
    val schoolYearPosition: String = "",
    val imageStyle: String = "cartoon",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class GalleryItem(
    val id: String,
    val playerId: String,
    val imageUrl: String,
    // English AI-generation phrase + short title
    val phraseEn: String,
    val titleEn: String,
    // Player-language display phrase + short title
    val phraseLocal: String,
    val titleLocal: String,
    // The 3 words in English (used for API calls)
    val wordCharacter: String,
    val wordAction: String,
    val wordPlace: String,
    // The 3 words in the player's language (for display)
    val wordCharacterLocal: String = "",
    val wordActionLocal: String = "",
    val wordPlaceLocal: String = "",
    val cost: Int = 100
)

@Serializable
data class AppSettings(
    val geminiApiKey: String = "",
    val apiValid: Boolean = false,
    val modelQuestions: String = "",
    val modelTranslation: String = "",
    val modelImagePrompt: String = "",
    val modelImageGeneration: String = "",
    val availableModelsJson: String = "",
    // Pollinations
    val pollinationsApiKey: String = "",
    val pollinationsKeyValid: Boolean = false,
    val pollinationsModel1: String = "kontext",
    val pollinationsModel2: String = "nova-canvas",
    // ModelScope (async provider)
    val modelScopeApiKey: String = "",
    val modelScopeKeyValid: Boolean = false,
    val modelScopeModelsJson: String = "",
    val modelScopeModel: String = "Qwen/Qwen-Image-2512",
    // Per-image-model enable flags (generation tries only enabled ones, in order)
    val enableGeminiImage: Boolean = true,
    val enablePollinations1: Boolean = true,
    val enablePollinations2: Boolean = true,
    val enableModelScope: Boolean = false
)

data class GeminiModel(
    val name: String,
    val displayName: String,
    val supportedMethods: List<String>
) {
    val isTextModel: Boolean get() = "generateContent" in supportedMethods
    val isImageModel: Boolean get() = "predict" in supportedMethods
    val isPreview: Boolean get() = "preview" in name.lowercase()
    val shortName: String get() = name.removePrefix("models/")
}

// Translated word lists for one language
data class WordTranslations(
    val language: String,
    val characters: List<String>,
    val actions: List<String>,
    val places: List<String>
)

// ── Quiz ─────────────────────────────────────────────────────────────────────

// A single question as returned by the AI and held in memory during the quiz
data class QuizQuestion(
    val id: String,
    val subject: String,
    val category: String,
    val level: Int,                  // 1 / 2 / 3 — also used as star value if correct
    val type: String,                // "multiple_choice" | "text" | "true_false"
    val instruction: String,
    val question: String,
    val options: List<String>?,      // only populated for multiple_choice
    val answer: String
)

// One answer record stored inside a completed Quiz
@Serializable
data class QuizAnswer(
    val id: String,
    val subject: String,
    val category: String,
    val level: Int,
    val type: String,
    val instruction: String,
    val question: String,
    val options: List<String>? = null,
    val correctAnswer: String,
    val playerAnswer: String,
    val wasCorrect: Boolean
)

// A fully submitted quiz persisted to storage
@Serializable
data class Quiz(
    val id: String,
    val playerId: String,
    val startedAt: Long,
    val submittedAt: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val starsEarned: Int,
    val bonusStars: Int = 0,
    val answers: List<QuizAnswer>
)

// ── Pollinations models available for selection ───────────────────────────────
val POLLINATIONS_MODELS = listOf(
    "kontext",
    "nova-canvas",
    "flux",
    "klein",
    "gptimage-large",
    "gptimage",
    "zimage"
)

// ── ModelScope models available for selection ─────────────────────────────────
val MODELSCOPE_MODELS = listOf(
    "Qwen/Qwen-Image-2512",
    "MusePublic/FLUX.1-Kontext-Dev",
    "flux-community/FLUX.2-klein-9B"
)
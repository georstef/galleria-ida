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
    val pollinationsModel3: String = "flux"
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

// ── Pollinations models available for selection ──────────────────────────────
val POLLINATIONS_MODELS = listOf(
    "kontext",
    "nova-canvas",
    "flux",
    "klein",
    "gptimage-large",
    "gptimage",
    "zimage"
)
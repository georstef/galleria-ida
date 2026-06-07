package com.galleriaida.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name : String,
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
    val title: String,
    val sentence: String,
    val wordsUsed: List<String>,
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
    val availableModelsJson: String = ""
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

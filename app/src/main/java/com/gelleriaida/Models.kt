package com.gelleriaida.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val schoolClass: String,
    val language: String,
    val stars: Int = 0,
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
    val apiValid: Boolean = false
)

package com.galleriaida.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.galleriaida.data.AppSettings
import com.galleriaida.data.GalleryItem
import com.galleriaida.data.Player
import com.galleriaida.data.Quiz
import com.galleriaida.data.WordTranslations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kidsapp_prefs")

class PreferencesManager(private val context: Context) {

    private val PLAYERS_KEY  = stringPreferencesKey("players")
    private val GALLERY_KEY  = stringPreferencesKey("gallery")
    private val SETTINGS_KEY = stringPreferencesKey("settings")
    private val QUIZZES_KEY  = stringPreferencesKey("quizzes")

    // ── Players ──────────────────────────────────────────────────────────────

    val playersFlow: Flow<List<Player>> = context.dataStore.data.map { prefs ->
        val json = prefs[PLAYERS_KEY] ?: "[]"
        runCatching { Json.decodeFromString<List<Player>>(json) }.getOrDefault(emptyList())
    }

    suspend fun savePlayers(players: List<Player>) {
        context.dataStore.edit { prefs ->
            prefs[PLAYERS_KEY] = Json.encodeToString(players)
        }
    }

    // ── Gallery ──────────────────────────────────────────────────────────────

    val galleryFlow: Flow<List<GalleryItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[GALLERY_KEY] ?: "[]"
        runCatching { Json.decodeFromString<List<GalleryItem>>(json) }.getOrDefault(emptyList())
    }

    suspend fun saveGallery(items: List<GalleryItem>) {
        context.dataStore.edit { prefs ->
            prefs[GALLERY_KEY] = Json.encodeToString(items)
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val json = prefs[SETTINGS_KEY] ?: "{}"
        runCatching { Json.decodeFromString<AppSettings>(json) }.getOrDefault(AppSettings())
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = Json.encodeToString(settings)
        }
    }

    // ── Quizzes ──────────────────────────────────────────────────────────────

    val quizzesFlow: Flow<List<Quiz>> = context.dataStore.data.map { prefs ->
        val json = prefs[QUIZZES_KEY] ?: "[]"
        runCatching { Json.decodeFromString<List<Quiz>>(json) }.getOrDefault(emptyList())
    }

    suspend fun saveQuizzes(quizzes: List<Quiz>) {
        context.dataStore.edit { prefs ->
            prefs[QUIZZES_KEY] = Json.encodeToString(quizzes)
        }
    }

    // ── Word translation cache (per language, stored as JSON file) ───────────

    private fun translationFile(language: String): java.io.File {
        val dir = java.io.File(context.filesDir, "word_translations").also { it.mkdirs() }
        return java.io.File(dir, "${language.lowercase()}.json")
    }

    fun loadWordTranslations(language: String): WordTranslations? {
        return try {
            val file = translationFile(language)
            if (!file.exists()) return null
            val j    = org.json.JSONObject(file.readText())
            fun arr(key: String): List<String> {
                val a = j.getJSONArray(key)
                return (0 until a.length()).map { a.getString(it) }
            }
            WordTranslations(
                language   = language,
                characters = arr("characters"),
                actions    = arr("actions"),
                places     = arr("places")
            )
        } catch (e: Exception) {
            android.util.Log.e("PreferencesManager", "loadWordTranslations error: ${e.message}")
            null
        }
    }

    fun saveWordTranslations(translations: WordTranslations) {
        try {
            val j = org.json.JSONObject().apply {
                put("characters", org.json.JSONArray(translations.characters))
                put("actions",    org.json.JSONArray(translations.actions))
                put("places",     org.json.JSONArray(translations.places))
            }
            translationFile(translations.language).writeText(j.toString())
            android.util.Log.d("PreferencesManager", "Word translations saved for ${translations.language}")
        } catch (e: Exception) {
            android.util.Log.e("PreferencesManager", "saveWordTranslations error: ${e.message}")
        }
    }
}
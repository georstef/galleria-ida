package com.gelleriaida.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gelleriaida.data.AppSettings
import com.gelleriaida.data.GalleryItem
import com.gelleriaida.data.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kidsapp_prefs")

class PreferencesManager(private val context: Context) {

    private val PLAYERS_KEY = stringPreferencesKey("players")
    private val GALLERY_KEY = stringPreferencesKey("gallery")
    private val SETTINGS_KEY = stringPreferencesKey("settings")

    val playersFlow: Flow<List<Player>> = context.dataStore.data.map { prefs ->
        val json = prefs[PLAYERS_KEY] ?: "[]"
        runCatching { Json.decodeFromString<List<Player>>(json) }.getOrDefault(emptyList())
    }

    val galleryFlow: Flow<List<GalleryItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[GALLERY_KEY] ?: "[]"
        runCatching { Json.decodeFromString<List<GalleryItem>>(json) }.getOrDefault(emptyList())
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val json = prefs[SETTINGS_KEY] ?: "{}"
        runCatching { Json.decodeFromString<AppSettings>(json) }.getOrDefault(AppSettings())
    }

    suspend fun savePlayers(players: List<Player>) {
        context.dataStore.edit { prefs ->
            prefs[PLAYERS_KEY] = Json.encodeToString(players)
        }
    }

    suspend fun saveGallery(items: List<GalleryItem>) {
        context.dataStore.edit { prefs ->
            prefs[GALLERY_KEY] = Json.encodeToString(items)
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = Json.encodeToString(settings)
        }
    }
}

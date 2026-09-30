package com.gopu.arrow.puzzle.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arrow_puzzle_preferences")

object DataStoreHelper {
    private val HIGHEST_UNLOCKED = intPreferencesKey("highest_unlocked_level")
    private val BEST_STARS = stringSetPreferencesKey("best_stars")
    private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    private val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")

    suspend fun saveHighestUnlockedLevel(context: Context, level: Int) {
        context.dataStore.edit { prefs ->
            prefs[HIGHEST_UNLOCKED] = level
        }
    }

    fun observeHighestUnlockedLevel(context: Context): Flow<Int> {
        return context.dataStore.data.map { it[HIGHEST_UNLOCKED] ?: 1 }
    }

    suspend fun saveBestStars(context: Context, levelId: String, stars: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[BEST_STARS]?.toList()?.toMutableList() ?: mutableListOf()
            val entry = "$levelId:$stars"
            current.removeAll { it.startsWith("$levelId:") }
            current.add(entry)
            prefs[BEST_STARS] = current.toSet()
        }
    }

    fun observeBestStars(context: Context): Flow<Map<String, Int>> {
        return context.dataStore.data.map { prefs ->
            prefs[BEST_STARS]?.toList()?.associate {
                val parts = it.split(":")
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } ?: emptyMap()
        }
    }

    suspend fun saveSoundEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SOUND_ENABLED] = enabled
        }
    }

    fun observeSoundEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { it[SOUND_ENABLED] ?: true }
    }

    suspend fun saveHapticsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAPTICS_ENABLED] = enabled
        }
    }

    fun observeHapticsEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { it[HAPTICS_ENABLED] ?: true }
    }

    suspend fun saveTutorialSeen(context: Context, seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TUTORIAL_SEEN] = seen
        }
    }

    fun observeTutorialSeen(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { it[TUTORIAL_SEEN] ?: false }
    }
}

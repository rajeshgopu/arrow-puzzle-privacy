package com.gopu.arrow.puzzle.game.data

import android.content.Context
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.levels.LevelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ProgressRepository(private val context: Context) {
    val highestUnlockedLevel: Flow<Int> = DataStoreHelper.observeHighestUnlockedLevel(context)
    val bestStars: Flow<Map<String, Int>> = DataStoreHelper.observeBestStars(context)
    val soundEnabled: Flow<Boolean> = DataStoreHelper.observeSoundEnabled(context)
    val hapticsEnabled: Flow<Boolean> = DataStoreHelper.observeHapticsEnabled(context)
    val tutorialSeen: Flow<Boolean> = DataStoreHelper.observeTutorialSeen(context)

    suspend fun unlockLevel(levelId: String, stars: Int, levelOrder: Int) {
        val current = highestUnlockedLevel.first()
        if (levelOrder > current) {
            DataStoreHelper.saveHighestUnlockedLevel(context, levelOrder)
        }
        val best = bestStars.first()[levelId] ?: 0
        if (stars > best) {
            DataStoreHelper.saveBestStars(context, levelId, stars)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) = DataStoreHelper.saveSoundEnabled(context, enabled)
    suspend fun setHapticsEnabled(enabled: Boolean) = DataStoreHelper.saveHapticsEnabled(context, enabled)
    suspend fun setTutorialSeen(seen: Boolean) = DataStoreHelper.saveTutorialSeen(context, seen)

    fun isLevelUnlocked(levelOrder: Int): Boolean = runBlocking { highestUnlockedLevel.first() >= levelOrder }
}

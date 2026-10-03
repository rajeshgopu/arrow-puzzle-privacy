package com.gopu.arrow.puzzle.game.data

import android.content.Context
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.levels.LevelRepository
import com.gopu.arrow.puzzle.game.ui.components.LaunchAnimationStyle
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

    /** Which departure animation a newly launched arrow plays. Presentation only. */
    val launchAnimation: Flow<LaunchAnimationStyle> =
        DataStoreHelper.observeLaunchAnimation(context)

    /**
     * The language the player picked in Settings, as a BCP 47 tag, or null when
     * they have not picked one and the device locale decides.
     *
     * Presentation only, and deliberately a separate key from everything above:
     * this is the one setting that changes what the UI *says*, so it is also the
     * one setting that must not be able to touch progress. `setLanguageTag`
     * writes one key and nothing else.
     */
    val languageTag: Flow<String?> = DataStoreHelper.observeLanguageTag(context)

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

    suspend fun setLaunchAnimation(style: LaunchAnimationStyle) =
        DataStoreHelper.saveLaunchAnimation(context, style)

    /** Pass null to go back to following the device locale. */
    suspend fun setLanguageTag(tag: String?) = DataStoreHelper.saveLanguageTag(context, tag)

    fun isLevelUnlocked(levelOrder: Int): Boolean = runBlocking { highestUnlockedLevel.first() >= levelOrder }
}

package com.gopu.arrowpuzzle.ads

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns ad pacing rules and forwards requests to the bound [AdController].
 *
 * Interstitial rule: at most one interstitial after every
 * [interstitialInterval] completed levels. The counter lives for the app
 * session so a burst of quick wins cannot spam a full-screen ad.
 */
class AdHost(
    private val controller: AdController,
    private val interstitialInterval: Int = 2
) {
    private var completionsSinceInterstitial = 0

    val bannerAvailable: StateFlow<Boolean> get() = controller.bannerAvailable
    val rewardedAvailable: StateFlow<Boolean> get() = controller.rewardedAvailable

    /** Called once when a level is completed; shows a paced interstitial. */
    suspend fun onLevelCompleted() {
        completionsSinceInterstitial += 1
        if (completionsSinceInterstitial >= interstitialInterval) {
            completionsSinceInterstitial = 0
            controller.showInterstitial()
        }
    }

    suspend fun showRewarded(): Boolean = controller.showRewarded()
}

/** Google's public sample unit IDs; replace with production IDs before release. */
object AdUnitIds {
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
}

/**
 * Binds the AdMob provider, falling back to [NoAds] when the SDK is unavailable
 * so a missing ad network never blocks the game.
 */
fun defaultAdHost(context: Context): AdHost = AdHost(
    controller = runCatching {
        AdMobController(
            context = context.applicationContext,
            activityProvider = { context.findActivity() as? Activity },
            rewardedUnitId = AdUnitIds.REWARDED,
            interstitialUnitId = AdUnitIds.INTERSTITIAL
        )
    }.getOrElse { NoAds }
)

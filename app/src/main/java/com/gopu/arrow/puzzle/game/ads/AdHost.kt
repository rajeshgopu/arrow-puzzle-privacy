package com.gopu.arrow.puzzle.game.ads

import android.app.Activity
import android.content.Context
import android.view.View
import com.gopu.arrow.puzzle.game.BuildConfig
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

    /** Whether ads may be requested yet; false until consent is settled. */
    val canRequestAds: StateFlow<Boolean> get() = controller.canRequestAds

    /** Creates the banner view for a slot, or null when ads are not available. */
    fun createBannerView(context: Context): View? = controller.createBannerView(context)

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

/**
 * Ad unit IDs for this build. Debug builds resolve to Google's public test
 * units from `BuildConfig`, so no development or manual testing ever serves or
 * clicks a live ad; release builds resolve to this app's own units.
 */
object AdUnitIds {
    val REWARDED: String = BuildConfig.ADMOB_REWARDED_UNIT_ID
    val INTERSTITIAL: String = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
    val BANNER: String = BuildConfig.ADMOB_BANNER_UNIT_ID
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
            bannerUnitId = AdUnitIds.BANNER,
            rewardedUnitId = AdUnitIds.REWARDED,
            interstitialUnitId = AdUnitIds.INTERSTITIAL
        )
    }.getOrElse { NoAds }
)
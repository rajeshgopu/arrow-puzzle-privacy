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
 * Ad unit IDs per build type. Debug builds always use Google's public test
 * units so no development or manual testing ever serves or clicks a live ad;
 * release builds use this app's own units.
 */
object AdUnitIds {
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"

    private const val LIVE_REWARDED = "ca-app-pub-3319834061576964/1474813821"
    private const val LIVE_INTERSTITIAL = "ca-app-pub-3319834061576964/2352781229"
    private const val LIVE_BANNER = "ca-app-pub-3319834061576964/5314734915"

    val REWARDED: String = if (BuildConfig.DEBUG) TEST_REWARDED else LIVE_REWARDED
    val INTERSTITIAL: String = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else LIVE_INTERSTITIAL
    val BANNER: String = if (BuildConfig.DEBUG) TEST_BANNER else LIVE_BANNER
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
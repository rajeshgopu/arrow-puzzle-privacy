package com.gopu.arrowpuzzle.ads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Seam between the puzzle UI and whatever ad provider is bound. The UI only
 * ever asks whether a format is available and to show it; it never touches an
 * SDK directly, and a missing or failed ad never blocks progression.
 */
interface AdController {
    /** Whether a banner slot can currently be filled. */
    val bannerAvailable: StateFlow<Boolean>

    /** Whether an opt-in rewarded video is ready to offer. */
    val rewardedAvailable: StateFlow<Boolean>

    /** Show a full-screen interstitial. Resolves once the attempt is done. */
    suspend fun showInterstitial()

    /** Show a rewarded video. Returns true only when the reward was earned. */
    suspend fun showRewarded(): Boolean
}

/** Default provider: no ads, nothing blocked. */
object NoAds : AdController {
    override val bannerAvailable: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val rewardedAvailable: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override suspend fun showInterstitial() = Unit
    override suspend fun showRewarded(): Boolean = false
}

/**
 * Offline stand-in that mimics ad playback latency so the rewarded-continue flow
 * can be exercised without the SDK. Used only when AdMob cannot be initialized.
 */
class SimulatedAdController : AdController {
    override val bannerAvailable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    override val rewardedAvailable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    override suspend fun showInterstitial() {
        delay(1_200)
    }
    override suspend fun showRewarded(): Boolean {
        delay(1_600)
        return true
    }
}

package com.gopu.arrow.puzzle.game.ads

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Seam between the puzzle UI and whatever ad provider is bound. The UI only
 * ever asks whether a format is available and to show it; it never touches an
 * SDK directly, and a missing or failed ad never blocks progression.
 */
interface AdController {
    /** Whether ads may be requested at all; false until consent is settled. */
    val canRequestAds: StateFlow<Boolean>

    /** Whether a banner slot can currently be filled. */
    val bannerAvailable: StateFlow<Boolean>

    /** Whether an opt-in rewarded video is ready to offer. */
    val rewardedAvailable: StateFlow<Boolean>

    /**
     * Creates the banner view for a slot, already sized and loading. Only call
     * once [canRequestAds] is true. The caller owns the returned view.
     */
    fun createBannerView(context: Context): View?

    /** Show a full-screen interstitial. Resolves once the attempt is done. */
    suspend fun showInterstitial()

    /** Show a rewarded video. Returns true only when the reward was earned. */
    suspend fun showRewarded(): Boolean
}

/** Default provider: no ads, nothing blocked. */
object NoAds : AdController {
    override val canRequestAds: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val bannerAvailable: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val rewardedAvailable: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override fun createBannerView(context: Context): View? = null
    override suspend fun showInterstitial() = Unit
    override suspend fun showRewarded(): Boolean = false
}
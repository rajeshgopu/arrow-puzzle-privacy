package com.gopu.arrowpuzzle.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Real AdMob-backed provider. Rewarded availability is only advertised once an
 * ad is actually loaded, so the "+1 life" offer is hidden whenever no video is
 * ready. Every failure path safely resolves without blocking play.
 */
class AdMobController(
    private val context: Context,
    private val activityProvider: () -> Activity?,
    private val rewardedUnitId: String,
    private val interstitialUnitId: String
) : AdController {

    private val _rewardedAvailable = MutableStateFlow(false)
    override val rewardedAvailable: StateFlow<Boolean> = _rewardedAvailable.asStateFlow()

    // Banner slots stay off until an AdView is bound.
    private val _bannerAvailable = MutableStateFlow(false)
    override val bannerAvailable: StateFlow<Boolean> = _bannerAvailable.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    init {
        runCatching {
            MobileAds.initialize(context) {
                loadRewarded()
                loadInterstitial()
            }
        }
    }

    private fun loadRewarded() {
        runCatching {
            RewardedAd.load(
                context,
                rewardedUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        _rewardedAvailable.value = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        _rewardedAvailable.value = false
                    }
                }
            )
        }
    }

    private fun loadInterstitial() {
        runCatching {
            InterstitialAd.load(
                context,
                interstitialUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        }
    }

    override suspend fun showRewarded(): Boolean {
        val ad = rewardedAd ?: return false
        val activity = activityProvider() ?: return false

        rewardedAd = null
        _rewardedAvailable.value = false

        return suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewarded()
                    if (cont.isActive) cont.resume(earned)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadRewarded()
                    if (cont.isActive) cont.resume(false)
                }
            }
            ad.show(activity) { earned = true }
        }
    }

    override suspend fun showInterstitial() {
        val ad = interstitialAd
        val activity = activityProvider()
        if (ad == null || activity == null) {
            loadInterstitial()
            return
        }

        interstitialAd = null
        suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            ad.show(activity)
        }
    }
}

/** Walks the context chain to find the hosting [Activity], if any. */
fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

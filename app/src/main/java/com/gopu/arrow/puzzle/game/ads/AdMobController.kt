package com.gopu.arrow.puzzle.game.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Real AdMob-backed provider. Nothing is requested until [AdConsent] reports
 * that ads are allowed, so the SDK is never initialized and no ad is loaded
 * ahead of the user's answer. Rewarded availability is only advertised once an
 * ad is actually loaded, so the "+1 life" offer is hidden whenever no video is
 * ready. Every failure path safely resolves without blocking play.
 */
class AdMobController(
    private val context: Context,
    private val activityProvider: () -> Activity?,
    private val bannerUnitId: String,
    private val rewardedUnitId: String,
    private val interstitialUnitId: String
) : AdController {

    override val canRequestAds: StateFlow<Boolean> get() = AdConsent.canRequestAds

    private val _rewardedAvailable = MutableStateFlow(false)
    override val rewardedAvailable: StateFlow<Boolean> = _rewardedAvailable.asStateFlow()

    // Banner slots stay empty until a slot creates its AdView.
    private val _bannerAvailable = MutableStateFlow(false)
    override val bannerAvailable: StateFlow<Boolean> = _bannerAvailable.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    /** Banner attempts so far, so a failing slot retries a bounded number of times. */
    private var bannerAttempts = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            AdConsent.canRequestAds.filter { it }.first()
            runCatching {
                MobileAds.initialize(context) {
                    Log.i(Tag, "MobileAds initialized")
                    loadRewarded()
                    loadInterstitial()
                }
            }.onFailure { Log.w(Tag, "MobileAds.initialize failed", it) }
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
                        Log.w(Tag, "Rewarded load failed: ${error.describe()}")
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
                        Log.w(Tag, "Interstitial load failed: ${error.describe()}")
                    }
                }
            )
        }
    }

    override fun createBannerView(context: Context): View {
        val adView = AdView(context)
        adView.adUnitId = bannerUnitId
        adView.setAdSize(
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                context.resources.displayMetrics.widthPixels
            )
        )
        adView.setAdListener(object : AdListener() {
            override fun onAdLoaded() {
                bannerAttempts = 0
                _bannerAvailable.value = true
                Log.i(Tag, "Banner loaded ($bannerUnitId)")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                _bannerAvailable.value = false
                /*
                 * The view stays alive so the slot keeps its measured size, and
                 * the request is retried a bounded number of times: a banner
                 * that failed once (cold start, no network) otherwise stays
                 * blank for the rest of the session.
                 */
                Log.w(Tag, "Banner load failed (attempt $bannerAttempts): ${error.describe()}")
                if (bannerAttempts < MaxBannerAttempts) {
                    bannerAttempts += 1
                    scope.launch {
                        delay(BannerRetryDelayMs * bannerAttempts)
                        adView.loadAd(AdRequest.Builder().build())
                    }
                }
            }
        })
        bannerAttempts = 1
        adView.loadAd(AdRequest.Builder().build())
        return adView
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

private const val Tag = "Ads"

/** Bounded retries so a transient banner failure is not blank for the session. */
private const val MaxBannerAttempts = 3
private const val BannerRetryDelayMs = 2_000L

private fun LoadAdError.describe(): String = "code=$code domain=$domain message=$message"
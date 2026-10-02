package com.gopu.arrow.puzzle.game.ads

import android.app.Activity
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.gopu.arrow.puzzle.game.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ads may be requested once consent was obtained or is not required here. */
private fun ConsentInformation.allowsAds(): Boolean =
    consentStatus == ConsentInformation.ConsentStatus.OBTAINED ||
        consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED

/**
 * UMP consent gate. [canRequestAds] stays false until Google's consent state
 * says ads may be requested, and no ad is requested before that. Debug builds
 * start out allowed because a test build has no real users to protect.
 *
 * The gate is re-read on every foreground pass, so a form that is answered,
 * dismissed or left open across a pause is still picked up, and a user who
 * said no is asked again on the next launch rather than being stuck off for
 * the life of the install. The flow only ever moves on Google's answer: it
 * never flips to true on its own.
 */
object AdConsent {
    private val _canRequestAds = MutableStateFlow(BuildConfig.DEBUG)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /**
     * The activity a consent round-trip is currently waiting on. Guards against
     * a second form while one is already up, and is discarded as stale once
     * that activity is gone, so a callback that never arrives cannot wedge the
     * gate shut for later launches.
     */
    private var pending: Activity? = null

    /**
     * Requests the consent state and shows the UMP form when the network
     * requires it. Safe to call on every foreground: it does nothing once ads
     * are allowed, and does not stack forms. Entirely best-effort - if the
     * update fails, ads stay gated off rather than being requested without an
     * answer.
     */
    fun request(activity: Activity) {
        if (_canRequestAds.value) return
        val inFlight = pending
        if (inFlight != null) {
            if (!inFlight.isDestroyed) return
            pending = null
        }
        pending = activity

        runCatching {
            val params = ConsentRequestParameters.Builder().build()
            val information = UserMessagingPlatform.getConsentInformation(activity)
            information.requestConsentInfoUpdate(
                activity,
                params,
                {
                    if (information.isConsentFormAvailable) {
                        UserMessagingPlatform.loadConsentForm(
                            activity,
                            { form -> form.show(activity) { settle(information) } },
                            { settle(information) }
                        )
                    } else {
                        settle(information)
                    }
                },
                { pending = null }
            )
        }.onFailure { pending = null }
    }

    /**
     * Opens the consent form from Settings so a player can change an answer at
     * any time, which is what the Play Store's privacy-options entry requires.
     * [onResult] reports whether a form was actually shown; it is false when
     * consent is not required in this region, so the caller can say so instead
     * of appearing to do nothing.
     */
    fun showPrivacyOptions(activity: Activity, onResult: (formShown: Boolean) -> Unit = {}) {
        if (pending?.isDestroyed == false) {
            onResult(false)
            return
        }
        pending = activity

        runCatching {
            val information = UserMessagingPlatform.getConsentInformation(activity)
            if (!information.isConsentFormAvailable) {
                // Nothing to ask; record whatever Google already decided so a
                // revoked or expired status still gates ads off.
                settle(information)
                onResult(false)
                return
            }
            UserMessagingPlatform.loadConsentForm(
                activity,
                { form: ConsentForm -> form.show(activity) { settle(information); onResult(true) } },
                { pending = null; onResult(false) }
            )
        }.onFailure { pending = null; onResult(false) }
    }

    /** Records Google's answer and frees the gate for the next request. */
    private fun settle(information: ConsentInformation) {
        pending = null
        _canRequestAds.value = information.allowsAds()
    }
}

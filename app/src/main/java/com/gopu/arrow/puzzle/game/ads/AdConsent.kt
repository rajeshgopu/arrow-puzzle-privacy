package com.gopu.arrow.puzzle.game.ads

import android.app.Activity
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
 */
object AdConsent {
    private val _canRequestAds = MutableStateFlow(BuildConfig.DEBUG)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /**
     * Requests the consent state and shows the UMP form when the network
     * requires it. Entirely best-effort: if the update fails, ads stay gated
     * off for this session rather than being requested without an answer.
     */
    fun request(activity: Activity) {
        if (_canRequestAds.value) return
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
                            { form ->
                                form.show(activity) { _canRequestAds.value = information.allowsAds() }
                            },
                            { _canRequestAds.value = information.allowsAds() }
                        )
                    } else {
                        _canRequestAds.value = information.allowsAds()
                    }
                },
                { }
            )
        }
    }
}
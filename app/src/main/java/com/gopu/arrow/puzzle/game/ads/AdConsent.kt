package com.gopu.arrow.puzzle.game.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Requests the consent state and shows the UMP form when the network requires
 * it. Entirely best-effort: any failure simply proceeds, since the game never
 * depends on ads to function.
 */
object AdConsent {
    fun request(activity: Activity) {
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
                            { form -> form.show(activity) { } },
                            { }
                        )
                    }
                },
                { }
            )
        }
    }
}

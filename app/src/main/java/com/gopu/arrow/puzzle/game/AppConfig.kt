package com.gopu.arrow.puzzle.game

import android.app.Activity
import com.gopu.arrow.puzzle.game.ads.AdConsent

/**
 * Release-time configuration that must be filled in before Play submission.
 * Kept dependency-free so both the app and UI layers can read it.
 */
object AppConfig {
    /**
     * Public privacy-policy URL required by the Play Store and the in-app
     * Settings entry. It defaults to the published page the Play listing and
     * Data safety forms use, and can be moved without editing Kotlin by setting
     * `privacyPolicyUrl` in local.properties (or passing
     * -PprivacyPolicyUrl=...); the row is hidden rather than pointing at a
     * placeholder while it is blank.
     */
    val privacyPolicyUrl: String? =
        BuildConfig.PRIVACY_POLICY_URL.takeIf { it.isNotBlank() }

    /**
     * Whether User Messaging Platform consent is integrated and the persistent
     * Privacy Options entry should be shown. UMP is bound, so the entry is
     * offered: Play requires a way to revisit the consent answer.
     */
    const val privacyOptionsAvailable: Boolean = true

    /**
     * Reopens the UMP consent form. Returns whether a form was actually shown,
     * so the caller can report that consent is not required in this region
     * instead of looking unresponsive.
     */
    fun openPrivacyOptions(
        activity: Activity,
        onResult: (formShown: Boolean) -> Unit = {}
    ): Unit = AdConsent.showPrivacyOptions(activity, onResult)
}

package com.gopu.arrow.puzzle.game

/**
 * Release-time configuration that must be filled in before Play submission.
 * Kept dependency-free so both the app and UI layers can read it.
 */
object AppConfig {
    /**
     * Public privacy-policy URL required by the Play Store and the in-app
     * Settings entry. Set this before release; while it is null the Privacy
     * Policy row is hidden rather than pointing at a placeholder.
     */
    val privacyPolicyUrl: String? = null

    /**
     * Whether User Messaging Platform consent is integrated and the persistent
     * Privacy Options entry should be shown. Flip to true with Phase 4 ads.
     */
    const val privacyOptionsAvailable: Boolean = false
}

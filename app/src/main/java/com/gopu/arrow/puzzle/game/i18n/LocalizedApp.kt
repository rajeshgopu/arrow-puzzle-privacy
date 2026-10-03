package com.gopu.arrow.puzzle.game.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.Flow

/**
 * The context the activity handed us, before any language override.
 *
 * Anything long-lived goes here rather than through [LocalContext]: the ad
 * controller owns a banner view and a consent gate, and rebuilding either of
 * those because a player switched language would be a far worse outcome than
 * re-reading a few strings. [Context.createConfigurationContext] wraps this
 * context, so [com.gopu.arrow.puzzle.game.ads.findActivity] still walks up to
 * the activity through the overridden one.
 */
val LocalAppContext: ProvidableCompositionLocal<Context> =
    staticCompositionLocalOf { error("LocalAppContext provided outside LocalizedApp") }

/**
 * The language actually being rendered, or null when the device language is not
 * one this build ships.
 *
 * Null means the resource system is resolving to `res/values` - English - so
 * this is the "English fallback" signal as well as the "unsupported device
 * language" one. Only the interface font reads it, to decide whether the
 * bundled Latin fonts can draw what is on screen.
 */
val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage?> = staticCompositionLocalOf { null }

/**
 * Resolves the game's language for this composition and hands the rest of the
 * tree a context whose resources already point at it.
 *
 * How a language is chosen, in order:
 *  1. the tag the player picked in Settings, if it is still a language this
 *     build ships ([AppLanguage.fromTag]);
 *  2. otherwise the device locale, which the resource system resolves on its
 *     own and which falls back to English when it is not translated.
 *
 * That order is also why there is no "detect once on first launch" step: the
 * device locale is read live, so a player who changes their phone's language
 * while the app is installed gets the right thing without reinstalling, and
 * there is no stored flag that can disagree with the device.
 *
 * The override is a configuration overlay on the composition, not an activity
 * restart. Changing the language therefore recomposes the strings and nothing
 * else: no `recreate()`, so the board, the level in progress, the settings
 * toggles, the saved stars and the tutorial flag are all untouched, and no
 * screen has to be re-navigated to pick the new strings up.
 *
 * The one thing that does change is [LocalAppLanguage], which the theme reads
 * to pick the interface font.
 */
@Composable
fun LocalizedApp(
    languageTag: Flow<String?>,
    content: @Composable () -> Unit
) {
    val appContext = LocalContext.current
    val configuration = LocalConfiguration.current

    /*
     * Null until DataStore answers, which means "no choice stored" - the same
     * value a player who never opened Settings has. That resolves to the device
     * locale, which is already the right answer on the first frame. There is no
     * "still loading" state to distinguish, so there is no flash of the wrong
     * language, and the store read comfortably finishes behind the splash.
     */
    val storedTag by languageTag.collectAsState(initial = null)
    val stored = remember(storedTag) { AppLanguage.fromTag(storedTag) }

    /*
     * Null when the player has chosen nothing, and in that case the original
     * configuration is handed straight back rather than rebuilt from its own
     * first locale: a device configured with several languages relies on the
     * whole locale list for fallback, and collapsing it to one entry would
     * quietly change which resource folder wins.
     */
    val localized = remember(configuration, stored) {
        if (stored == null) {
            configuration
        } else {
            Configuration(configuration).apply {
                ConfigurationCompat.setLocales(this, LocaleListCompat.forLanguageTags(stored.tag))
                setLayoutDirection(stored.locale)
            }
        }
    }

    /*
     * Skipped entirely when there is no override, so the common case - a player
     * who has never changed the language - hands the very same context back and
     * Compose sees no change at all.
     */
    val localizedContext = remember(appContext, localized, stored) {
        if (stored == null) appContext else appContext.createConfigurationContext(localized)
    }

    /*
     * Read live rather than captured once, so a player who changes their
     * phone's language while the app is installed gets the right thing with no
     * reinstall and no stored flag left disagreeing with the device.
     */
    val language = remember(stored, localizedContext) {
        stored ?: AppLanguage.fromLocale(
            ConfigurationCompat.getLocales(localizedContext.resources.configuration)
                .takeIf { !it.isEmpty }
                ?.get(0)
        )
    }

    CompositionLocalProvider(
        LocalConfiguration provides localized,
        LocalContext provides localizedContext,
        LocalAppContext provides appContext,
        LocalAppLanguage provides language
    ) {
        content()
    }
}
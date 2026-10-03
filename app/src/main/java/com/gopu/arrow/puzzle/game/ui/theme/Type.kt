package com.gopu.arrow.puzzle.game.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.gopu.arrow.puzzle.game.R
import com.gopu.arrow.puzzle.game.i18n.LocalAppLanguage

/**
 * The Latin families, bundled and subset by `tools/fonts/build_fonts.py`.
 *
 * These are deliberately not exposed to the rest of the UI on their own: see
 * [UiSans] and [DisplaySerif], which decide between these and the system
 * family based on the language in play. [UiSansLatin] is the escape hatch for
 * the one place that must always use Poppins regardless of language - the
 * ARROW PUZZLE wordmark, which is a brand and never translated.
 */
private val PoppinsDisplay = FontFamily(
    Font(R.font.playfair_display_bold, FontWeight.Bold),
    Font(R.font.playfair_display_black, FontWeight.Black)
)

private val PoppinsInterface = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)

/**
 * Poppins, always. For text that is the Latin brand and not a translated
 * string: the wordmark, and the level and pack numerals that are read out of
 * level data rather than out of a resource.
 */
val UiSansLatin: FontFamily = PoppinsInterface

/** Playfair Display, always. See [UiSansLatin] for when this is correct. */
val DisplaySerifLatin: FontFamily = PoppinsDisplay

/**
 * The interface sans for every label, card and button.
 *
 * Poppins is subset to Latin, so it cannot draw Japanese or Korean. Rather than
 * bundle a second multi-megabyte CJK font to duplicate what the platform
 * already ships, [AppLanguage.needsSystemFont] routes those two locales to
 * [FontFamily.Default], which is the same family every TextView on Android
 * uses and which resolves CJK through the system font list on every supported
 * API level. The digits and symbols that remain in those locales - level
 * numbers, the version, the heart on the rewarded button - fall back with it
 * and are unaffected.
 *
 * `@ReadOnlyComposable` keeps this free to read from inside a `TextStyle`
 * expression in any composable, and it is the reason no call site had to change
 * to become locale-aware.
 */
val UiSans: FontFamily
    @Composable @ReadOnlyComposable get() =
        if (LocalAppLanguage.current?.needsSystemFont == true) FontFamily.Default else PoppinsInterface

/**
 * The display serif behind the large headline styles. Same CJK routing as
 * [UiSans]; the only headline is the wordmark, which does not go through here.
 */
val DisplaySerif: FontFamily
    @Composable @ReadOnlyComposable get() =
        if (LocalAppLanguage.current?.needsSystemFont == true) FontFamily.Default else PoppinsDisplay
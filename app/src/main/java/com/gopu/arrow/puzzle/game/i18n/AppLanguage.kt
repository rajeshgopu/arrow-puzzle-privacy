package com.gopu.arrow.puzzle.game.i18n

import com.gopu.arrow.puzzle.game.R
import java.util.Locale

/** The tag of the fallback language, which has no folder of its own. */
private const val FALLBACK_TAG = "en"

/**
 * A language the game is translated into.
 *
 * [tag] is the BCP 47 tag and is the only thing persisted: adding a language is
 * one entry here plus one `res/values-xx/strings.xml`, and nothing in the game
 * logic changes. [labelRes] is the endonym shown in the picker, because a player
 * scanning a list of languages is looking for their own name for it rather than
 * for an English gloss.
 *
 * English is deliberately *not* listed as the fallback here. "Follow the device"
 * and "follow English" are different choices: a player on an unsupported system
 * language gets English from the resource system without having selected
 * anything, and [fromLocale] reporting null is what tells the UI that.
 */
enum class AppLanguage(val tag: String, val labelRes: Int) {

    ENGLISH(FALLBACK_TAG, R.string.language_english),
    GERMAN("de", R.string.language_german),
    JAPANESE("ja", R.string.language_japanese),
    FRENCH("fr", R.string.language_french),
    KOREAN("ko", R.string.language_korean),
    SPANISH("es", R.string.language_spanish),

    /**
     * Brazilian Portuguese only. The tag carries the region so a pt-PT device
     * does not match it: `values-pt-rBR` is the Brazilian qualifier, and
     * European Portuguese stays on English until someone writes `values-pt`.
     */
    PORTUGUESE_BR("pt-BR", R.string.language_portuguese_br);

    /** The locale that makes the resource system pick this language's files. */
    val locale: Locale get() = Locale.forLanguageTag(tag)

    /**
     * The `res/values-xx` folder this language lives in, or null for English.
     *
     * English is the fallback and sits in plain `res/values`, not `values-en`:
     * that folder is what a device with an unsupported language resolves to, and
     * `values-en` would leave that case with nowhere to go.
     *
     * Android's legacy qualifier spells a region as `r` followed by the region in
     * upper case, so Brazilian Portuguese is `values-pt-rBR` and not
     * `values-pt-br` or `values-pt-BR`. Deriving it here rather than in the test
     * that checks it means the folder name has one definition.
     */
    val resourceQualifier: String?
        get() {
            if (tag == FALLBACK_TAG) return null
            val region = tag.substringAfter('-', "")
            if (region.isEmpty()) return "values-$tag"
            return "values-${tag.substringBefore('-')}-r$region"
        }

    /**
     * True for the scripts the bundled Latin fonts cannot draw at all.
     *
     * Poppins and Playfair Display are subset to Latin by
     * `tools/fonts/build_fonts.py`, so Japanese and Korean text needs a font
     * that has those glyphs. `ui/theme/Type.kt` reads this to swap the
     * interface family for the system one, which is how every other CJK app on
     * Android does it: shipping a second multi-megabyte font to duplicate what
     * the platform already has would grow the APK for nothing.
     */
    val needsSystemFont: Boolean get() = this == JAPANESE || this == KOREAN

    /** The language code on its own, for comparing against a device locale. */
    private val language: String get() = tag.substringBefore('-')

    /** The region on its own, or empty for a language that has no region. */
    private val region: String get() = tag.substringAfter('-', "")

    companion object {

        /**
         * The languages offered in Settings, in the order they are shown.
         * "System default" is not an entry: it is the absence of a stored
         * choice, so the picker offers it as a separate first row.
         */
        val selectable: List<AppLanguage> = entries.toList()

        /**
         * The language named by a persisted tag, or null when there is none.
         *
         * A null answer is expected rather than exceptional: it covers a fresh
         * install that has never chosen, and a stored tag from a build that
         * shipped a language this one does not have. Both fall back to the
         * device locale, so downgrading the app cannot strand a player in a
         * language that no longer exists.
         */
        fun fromTag(tag: String?): AppLanguage? {
            val stored = tag?.trim().orEmpty()
            if (stored.isEmpty()) return null
            return entries.firstOrNull { it.tag.equals(stored, ignoreCase = true) }
        }

        /**
         * The shipped language a device locale names, or null when it names
         * none - an unsupported language, or European Portuguese.
         *
         * Null does not mean English here. It means "leave the configuration
         * alone", and the resource system then resolves to `res/values`, which
         * is English. That is what makes an unsupported device language fall
         * back rather than show a blank string.
         */
        fun fromLocale(locale: Locale?): AppLanguage? {
            val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
            if (language.isEmpty()) return null
            val region = locale?.country?.uppercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { candidate ->
                candidate.language == language &&
                    (candidate.region.isEmpty() || candidate.region == region)
            }
        }
    }
}
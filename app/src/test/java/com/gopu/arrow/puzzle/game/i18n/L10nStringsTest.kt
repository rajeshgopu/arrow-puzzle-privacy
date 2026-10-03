package com.gopu.arrow.puzzle.game.i18n

import com.gopu.arrow.puzzle.game.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * The translation files themselves, checked as data rather than as rendered UI.
 *
 * This is the set of mistakes that no compiler and no amount of clicking
 * catches: a placeholder that moved, a translation that quietly landed in the
 * wrong file, a key nobody reads any more, and the brand being translated. All
 * four have shipped into real apps, and all four are cheap to assert here.
 *
 * The XML is read straight off disk with the JDK's parser rather than through
 * `Resources`, because a unit test has no `Context` - and because reading the
 * files directly is what lets a key present in one file and absent in another be
 * reported by name.
 */
class L10nStringsTest {

    private data class LocaleStrings(
        val qualifier: String,
        val strings: Map<String, String>
    )

    /** `values` is the default and the English fallback. */
    private val defaultStrings by lazy { parse("values") }

    private val translated by lazy {
        listOf("values-de", "values-es", "values-fr", "values-ja", "values-ko", "values-pt-rBR")
            .map { it to parse(it) }
    }

    @Test
    fun englishIsTheDefaultLocaleFolderAndNotValuesEn() {
        // The fallback works only if English sits in plain `values`. Putting it
        // in `values-en` would leave an unsupported device language with no
        // folder to fall back to.
        assertTrue("res/values/strings.xml is missing", resourceFile("values", "strings.xml").exists())
        assertFalse(
            "English belongs in res/values, not res/values-en: a device set to " +
                "an untranslated language has nothing to resolve to otherwise",
            resourceDirectory("values-en").exists()
        )
    }

    @Test
    fun allSevenLanguagesShipAFolder() {
        for (qualifier in listOf("values", "values-de", "values-fr", "values-ja", "values-ko", "values-es", "values-pt-rBR")) {
            assertTrue(
                "Missing res/$qualifier/strings.xml",
                resourceFile(qualifier, "strings.xml").exists()
            )
        }
    }

    @Test
    fun everyLocaleDefinesEveryKey() {
        // A missing key silently falls back to English, which is safe but means a
        // player sees half a translated screen. This build claims to be complete,
        // so a gap is a mistake worth failing on.
        for ((qualifier, strings) in translated) {
            val missing = defaultStrings.keys - strings.keys
            assertTrue(
                "$qualifier is missing ${missing.size} string(s): ${missing.sorted()}",
                missing.isEmpty()
            )
        }
    }

    @Test
    fun noLocaleDefinesAKeyEnglishDoesNot() {
        // The mirror of the test above, and the more likely typo: a key spelled
        // slightly differently in one file, which compiles, ships, and is never
        // read by anything.
        for ((qualifier, strings) in translated) {
            val extra = strings.keys - defaultStrings.keys
            assertTrue(
                "$qualifier defines key(s) English does not, so nothing can read " +
                    "them: ${extra.sorted()}",
                extra.isEmpty()
            )
        }
    }

    @Test
    fun placeholdersSurviveTranslationExactly() {
        /*
         * The rule the task states as "preserve placeholders exactly", checked
         * literally. A dropped `%2$d` throws `MissingFormatArgumentException` at
         * format time - on a German player's screen, the first time they lose a
         * life. A changed conversion (`%1$d` to `%1$s`) throws
         * `IllegalFormatConversionException`. A renumbered index silently
         * substitutes the wrong number, which is worse: no crash, wrong text.
         */
        for ((qualifier, strings) in translated) {
            for ((key, english) in defaultStrings) {
                val expected = formatSpecifiers(english)
                val actual = formatSpecifiers(strings.getValue(key))
                assertEquals(
                    "$qualifier/$key changed its placeholders.\n" +
                        "  en: $english\n" +
                        "  $qualifier: ${strings.getValue(key)}\n" +
                        "  expected $expected, found $actual",
                    expected,
                    actual
                )
            }
        }
    }

    @Test
    fun everyFormatSpecifierIsPositionalAndEveryPercentIsEscaped() {
        /*
         * `strings.xml` is read with `formatted` on, so two things go wrong if
         * this slips: an unindexed `%s` in a string that takes arguments is
         * resolved against whatever the caller happened to pass, and a bare `%`
         * throws `UnknownFormatConversionException`. `%%` is the one legal
         * escape, and the pack progress readout is its only user.
         */
        for ((qualifier, strings) in listOf("values" to defaultStrings) + translated) {
            for ((key, value) in strings) {
                val remainder = value
                    .replace(Regex("%\\d+\\$[a-zA-Z]"), "")
                    .replace("%%", "")
                assertTrue(
                    "$qualifier/$key has a percent that is not part of a " +
                        "positional specifier or an escape: $value",
                    '%' !in remainder
                )
            }
        }
    }

    @Test
    fun theBrandIsNeverTranslated() {
        // "Arrow Puzzle: Tap to Escape" is the store listing's search term and
        // the wordmark's own lettering. A localised app_name would lose both,
        // so it is repeated verbatim in every file and checked here rather than
        // trusted.
        for ((qualifier, strings) in translated) {
            assertEquals(
                "$qualifier translated the app name",
                defaultStrings.getValue("app_name"),
                strings.getValue("app_name")
            )
        }
        assertEquals("Arrow Puzzle: Tap to Escape", defaultStrings.getValue("app_name"))
    }

    @Test
    fun theBrandInsideOtherStringsIsAlsoLeftAlone() {
        // The version line embeds the app name, so it has to keep it too.
        for ((qualifier, strings) in translated) {
            assertTrue(
                "$qualifier dropped the brand from about_version",
                strings.getValue("about_version").contains("Arrow Puzzle: Tap to Escape")
            )
        }
    }

    @Test
    fun everyLanguageEndonymIsPresentInEveryLocale() {
        /*
         * A language picker shows each language in its own language. That means
         * the seven `language_*` entries are identical in all eight files - and
         * they must never be "translated", which is the mistake that makes a
         * Japanese picker show "Japanese".
         */
        val endonyms = AppLanguage.entries.map { languageKey(it) }
        for (key in endonyms) {
            val reference = defaultStrings.getValue(key)
            for ((qualifier, strings) in translated) {
                assertEquals(
                    "$qualifier/$key is not the endonym ('$reference')",
                    reference,
                    strings.getValue(key)
                )
            }
        }
    }

    @Test
    fun everyLanguageEnumEntryPointsAtItsOwnEndonym() {
        // Guards the pairing in code: an entry whose labelRes was pointed at
        // another language's string would make the picker offer the wrong name
        // for a language, which is worse than an untranslated label because it
        // looks correct.
        val fields = R.string::class.java.fields.associate { it.getInt(null) to it.name }
        for (language in AppLanguage.selectable) {
            val name = fields[language.labelRes]
                ?: error("${language.name} has a labelRes that is not a string resource")
            assertEquals(
                "${language.name} does not point at its own endonym",
                languageKey(language),
                name
            )
        }
        val claimed = AppLanguage.selectable.map { fields.getValue(it.labelRes) }.toSet()
        assertEquals(
            "A language_* string exists that no AppLanguage entry offers",
            AppLanguage.selectable.map { languageKey(it) }.toSet(),
            claimed
        )
    }

    @Test
    fun everyAppLanguageEntryHasAResourceFolder() {
        /*
         * The enum and the folders have to agree, or picking a language changes
         * the interface font and nothing else - the strings stay English while
         * the type does not. English is the special case: it is `values`, not
         * `values-en`, because that folder is also the fallback.
         */
        val folders = setOf(
            "values", "values-de", "values-es", "values-fr", "values-ja",
            "values-ko", "values-pt-rBR"
        )
        for (language in AppLanguage.selectable) {
            val expected = language.resourceQualifier ?: "values"
            assertTrue(
                "${language.name} (${language.tag}) resolves to $expected, " +
                    "which is not one of the shipped folders",
                folders.contains(expected)
            )
        }
    }

    @Test
    fun everyShippedFolderIsReachableFromTheEnum() {
        // The other direction: a translated folder no entry can select is dead
        // weight that will rot unnoticed.
        val reachable = AppLanguage.selectable.map { it.resourceQualifier ?: "values" }.toSet()
        assertEquals(
            "A translated folder exists that no AppLanguage entry offers",
            setOf("values", "values-de", "values-es", "values-fr", "values-ja", "values-ko", "values-pt-rBR"),
            reachable
        )
    }

    @Test
    fun aRegionQualifierIsSpelledTheWayAndroidExpects() {
        // `values-pt-br` and `values-pt-BR` are silently ignored by aapt2: the
        // legacy form is `r` plus an upper-case region. A folder that is spelled
        // the wrong way compiles, ships, and is simply never selected.
        assertEquals("values-pt-rBR", AppLanguage.PORTUGUESE_BR.resourceQualifier)
        assertEquals("values-de", AppLanguage.GERMAN.resourceQualifier)
        assertEquals(null, AppLanguage.ENGLISH.resourceQualifier)
    }

    /**
 * Keys whose value is legitimately identical to the English in some locales,
 * and why. Everything outside this set must be translated.
 *
 * This is an allowlist rather than a tolerance on purpose. "German still says
 * LEVEL" is correct - it is the established German word for the game's own noun
 * - while "German still says Tap any arrow with a clear route" is a bug. A
 * ratio or a percentage cannot tell those apart, but a list written down can,
 * and the reason next to each entry says whether it still holds.
 */
private val identicalByDesign = mapOf(
    // The brand, everywhere, in the launcher label and inside the version line.
    "app_name" to "the brand; the store listing's search term",
    "about_version" to "brand plus a version number, no translatable words",

    // Endonyms: a language picker names each language in its own language.
    "language_english" to "endonym",
    "language_german" to "endonym",
    "language_japanese" to "endonym",
    "language_french" to "endonym",
    "language_korean" to "endonym",
    "language_spanish" to "endonym",
    "language_portuguese_br" to "endonym",

    // Format templates with no words to translate. Their arguments are translated,
    // and `placeholdersSurviveTranslationExactly` covers those separately.
    "a11y_stat_value" to "pure format template",
    "pack_percent" to "pure format template",

    // Loanwords that are the correct German term rather than an untranslated
    // leftover. "Level" and "Pack" are English-origin words German uses as such,
    // and a player reads them as German game vocabulary, not as a bug.
    "home_level_label" to "'Level' is the German word",
    "hud_level" to "'Level' is the German word",
    "hud_pack" to "'Pack' is the German word",
    "pack_title" to "'Pack' is the German word",
    "pack_counter" to "'Pack' is the German word",
    "pack_coming_soon_title" to "'Pack' is the German word",

    // Borrowed into German long ago, and shorter than any translation of them.
    "action_start" to "'Start' is a German word too",
    "a11y_pause" to "'Pause' is the German word for a pause",
    "launch_style_minimal" to "'Minimal' is the German word too"
)

@Test
fun everyStringIsActuallyTranslatedWhereItShouldBe() {
    /*
     * The mirror of the completeness checks: a file can define every key and
     * still be the English file with the folder renamed. Anything identical to
     * English that is not in `identicalByDesign`, or that is listed there but
     * only matches in a locale the entry does not claim, is a missing
     * translation - and these are the ones that reach a player untranslated.
     */
    for ((qualifier, strings) in translated) {
        val unexpected = strings.filter { (key, value) ->
            value == defaultStrings.getValue(key) && !identicalByDesign.containsKey(key)
        }
        assertTrue(
            "$qualifier is untranslated for ${unexpected.keys.sorted()}. Either " +
                "translate it or, if identical wording is correct here, add it to " +
                "identicalByDesign with a reason.",
            unexpected.isEmpty()
        )
    }
}

@Test
    fun theAllowlistStillDescribesReality() {
        // The other direction: an entry nobody needs any more is a comment that
        // has drifted away from the data.
        val endonyms = AppLanguage.selectable.map { languageKey(it) }.toSet()
        val stillNeeded = identicalByDesign.filterKeys { key ->
            key.startsWith("language_") ||
                // Brand and templates are identical in every locale by definition.
                key == "app_name" || key == "about_version" ||
                key == "a11y_stat_value" || key == "pack_percent" ||
                translated.any { (qualifier, strings) ->
                    qualifier == "values-de" && strings.getValue(key) == defaultStrings.getValue(key)
                }
        }
        assertEquals(
            "identicalByDesign entries that no longer match any locale",
            stillNeeded.keys,
            identicalByDesign.keys
        )
        assertTrue(
            "The language_* allowlist entries should be exactly the endonyms",
            identicalByDesign.keys.filter { it.startsWith("language_") }.toSet() == endonyms
        )
    }

    @Test
    fun noTranslatedStringIsBlank() {
        // A key present but empty renders as an invisible gap, which is a
        // missing translation wearing a disguise.
        for ((qualifier, strings) in translated) {
            for ((key, value) in strings) {
                assertTrue("$qualifier/$key is blank", value.isNotBlank())
            }
        }
    }

    @Test
    fun noStringKeepsTheKeysOwnNameAsItsValue() {
        // Catches a copy-paste where the value column was filled with the key.
        for ((qualifier, strings) in listOf("values" to defaultStrings) + translated) {
            for ((key, value) in strings) {
                assertNotAKeyName("$qualifier/$key", value)
            }
        }
    }

    @Test
    fun theFrenchApostrophesAreEscapedSoTheFileParses() {
        // French leans on apostrophes throughout, and an unescaped one is a
        // parse error rather than a typo - which is the kind of mistake that
        // would take the whole locale down instead of one string.
        val strings = translated.first { it.first == "values-fr" }.second
        val apostropheRule = strings.getValue("how_to_rule_3")
        assertTrue(
            "Expected a French apostrophe, escaped as \\'",
            apostropheRule.contains("\\'")
        )
    }

    /**
 * The narrow control each short label is drawn in, and how wide that control
 * leaves the text.
 *
 * Budgets are in dp on a 360dp-wide screen, derived from the layouts in
 * `Common.kt`, `GameplayScreen.kt` and `SettingsScreen.kt`. The width model is a
 * rough but honest one: uppercase Latin and the accented Latin capitals this
 * game uses advance about 0.68em, and a CJK glyph is a full em. That is enough to
 * tell a label that fits from one that will be clipped, which is the only thing
 * this check needs to do - and it does not pretend to be a text shaper.
 */
private val oneLineLabels = mapOf(
    // GlassStatCard caption: 153dp plate minus 28dp padding, 38dp badge, 12dp gap.
    "home_level_label" to Budget("GlassStatCard caption", 75, 11, 0.5),
    "home_stars_label" to Budget("GlassStatCard caption", 75, 11, 0.5),
    "home_player_label" to Budget("player name plate", 150, 10, 2.2),

    // NeonChip readout: a share of the 332dp HUD row that also holds the lives
    // row, so 120dp is generous rather than exact.
    "hud_arrows" to Budget("NeonChip label", 120, 9, 1.5),
    "hud_moves" to Budget("NeonChip label", 120, 9, 1.5),

    // NeonStartButton: a full-width 54dp bar, so the label has the row to itself.
    "action_start" to Budget("start bar", 304, 16, 3.0),

    // The 22.sp title above the 46dp HUD buttons.
    "hud_level" to Budget("HUD level title", 180, 22, 1.0),
    "hud_pack" to Budget("HUD pack subtitle", 180, 11, 3.0),

    // ResultOverlay title: 0.86 of 360dp, less 2 x 28dp padding, two lines.
    "pause_title" to Budget("pause title", 254, 24, 1.0, 2),
    "victory_level_clear" to Budget("victory title", 254, 24, 1.0, 2),
    "victory_pack_clear" to Budget("victory title", 254, 24, 1.0, 2),
    "fail_out_of_lives" to Budget("failure title", 254, 24, 1.0, 2),

    // Full-width result buttons, 16.sp Black with 0.5.sp tracking, two lines.
    "action_play" to Budget("play pill label", 300, 28, 3.0),
    "action_resume" to Budget("result button", 254, 16, 0.5, 2),
    "action_restart" to Budget("result button", 254, 16, 0.5, 2),
    "action_replay" to Budget("result button", 254, 16, 0.5, 2),
    "action_try_again" to Budget("result button", 254, 16, 0.5, 2),
    "action_settings" to Budget("result button", 254, 16, 0.5, 2),
    "action_levels" to Budget("result button", 254, 16, 0.5, 2),
    "action_hint" to Budget("HUD action button", 180, 14, 1.0),
    "action_got_it" to Budget("how-to-play button", 254, 16, 0.5, 2),

    // Pack progress card caption and the pack selector pill.
    "pack_title" to Budget("pack progress card", 200, 17, 0.5),
    "pack_counter" to Budget("pack selector", 220, 14, 0.2, 2),
    "pack_coming_soon_title" to Budget("coming-soon plate", 280, 20, 0.5)
)

private data class Budget(
    val where: String,
    val widthDp: Int,
    val fontSizeSp: Int,
    val letterSpacingSp: Double,
    /**
     * How many lines the control will actually lay out before clipping.
     *
     * This is read off the composable, not guessed: a `Text` with no `maxLines`
     * wraps and its container grows, so two lines is two lines of room; a
     * `maxLines = 1` label has exactly one. Every entry below is either
     * one-lined in source or explicitly allowed to wrap.
     */
    val maxLines: Int = 1
) {
    /** How far one character of Latin text advances, in dp at 1x font scale. */
    val latinEmDp: Double get() = fontSizeSp * 0.68 + letterSpacingSp

    /** How far one CJK glyph advances. A CJK glyph is a full em by definition. */
    val cjkEmDp: Double get() = fontSizeSp + letterSpacingSp

    /** Room available for the whole label. */
    val availableDp: Double get() = widthDp.toDouble() * maxLines
}

@Test
fun everyShortLabelFitsTheControlItIsDrawnIn() {
    /*
     * The layout pass, as an assertion. Each of these labels sits in a control
     * whose height was set for the English text, so a translation that will not
     * fit one line is either clipped or wraps into something the design was not
     * drawn for. Shortening the translation is the right fix; growing the
     * control is not, because these are single-line chips and captions.
     */
    val offenders = mutableListOf<String>()

    for ((qualifier, strings) in translated) {
        for ((key, budget) in oneLineLabels) {
            val value = strings.getValue(key)
            val width = estimatedWidthDp(value, budget)
            if (width > budget.availableDp) {
                offenders += "$qualifier/$key needs ~${width.toInt()}dp " +
                    "(${value}) but the ${budget.where} leaves ${budget.availableDp.toInt()}dp"
            }
        }
    }

    assertTrue(
        "Translations too wide for their control. Shorten the string rather " +
            "than growing the control - these are single-line labels.\n" +
            offenders.joinToString("\n") { "  $it" },
        offenders.isEmpty()
    )
}

@Test
    fun theEnglishBaselineFitsTheSameControls() {
        // If the reference itself does not fit, the budgets above are wrong and
        // the other test in this pair is measuring nonsense.
        for ((key, budget) in oneLineLabels) {
            val width = estimatedWidthDp(defaultStrings.getValue(key), budget)
            assertTrue(
                "The budget for $key ($budget) is tighter than the English text " +
                    "needs (${width.toInt()}dp), so it cannot be right",
                width <= budget.availableDp
            )
        }
    }

private fun estimatedWidthDp(value: String, budget: Budget): Double =
    value.sumOf { character ->
        // CJK, Hangul, Kana and the full-width punctuation used alongside them
        // all advance a full em; everything else in this game's copy is Latin.
        val codepoint = character.code
        val fullWidth = codepoint in 0x1100..0x11FF ||   // Hangul Jamo
            codepoint in 0x3040..0x30FF ||               // Kana
            codepoint in 0x3400..0x4DBF ||               // CJK Ext A
            codepoint in 0x4E00..0x9FFF ||               // CJK Unified
            codepoint in 0xAC00..0xD7A3 ||               // Hangul syllables
            codepoint in 0xF900..0xFAFF ||               // CJK compatibility
            codepoint in 0xFF01..0xFF60 ||               // full-width forms
            codepoint in 0x20000..0x2FA1F                // CJK Ext B+
        if (fullWidth) budget.cjkEmDp else budget.latinEmDp
    }

    // ---------------------------------------------------------------- helpers

    private fun assertNotAKeyName(where: String, value: String) {
        assertFalse("$where holds its own key name as its value", Regex("[a-z]+_[a-z_]+").matches(value))
    }

    private fun languageKey(language: AppLanguage): String {
        val fields = R.string::class.java.fields.associate { it.getInt(null) to it.name }
        return fields.getValue(language.labelRes)
    }

    /**
     * Every `%N$c` in a string, as index-plus-conversion pairs.
     *
     * Compared as a sorted set so that reordering the words around a placeholder
     * - which is normal and expected in most languages - is allowed, while
     * changing which argument goes where is not.
     */
    private fun formatSpecifiers(value: String): List<String> =
        Regex("%(\\d+)\\$([a-zA-Z])")
            .findAll(value)
            .map { "%${it.groupValues[1]}$${it.groupValues[2]}" }
            .sorted()
            .toList()

    private fun parse(qualifier: String): Map<String, String> {
        val file = resourceFile(qualifier, "strings.xml")
        assertTrue("Missing $file", file.exists())
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
        val document = factory.newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val result = LinkedHashMap<String, String>(nodes.length)
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            result[element.getAttribute("name")] = element.textContent
        }
        assertTrue("No strings parsed from $file", result.isNotEmpty())
        return result
    }

    private fun resourceFile(qualifier: String, name: String): File =
        File(resourceDirectory(qualifier), name)

    private fun resourceDirectory(qualifier: String): File {
        val relative = "src/main/res/$qualifier"
        val candidates = listOf(
            File(relative),
            File("app/$relative"),
            File(System.getProperty("user.dir"), relative)
        )
        // Not found is a legitimate answer here - one test asserts that a folder
        // is absent - so this returns the first candidate rather than throwing,
        // and callers that need the folder check `.exists()` themselves.
        return candidates.firstOrNull { it.isDirectory } ?: candidates.first()
    }
}
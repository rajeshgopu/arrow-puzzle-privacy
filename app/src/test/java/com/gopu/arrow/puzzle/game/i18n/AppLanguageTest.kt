package com.gopu.arrow.puzzle.game.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The language resolution rules, which are the whole of the "how does this app
 * pick a language" answer.
 *
 * There is no Android framework here on purpose: the decisions that matter - what
 * a stored tag means, what a device locale means, and what happens when neither
 * is a language this build ships - are all pure functions of a locale and a
 * string, and they are the ones that silently get wrong.
 */
class AppLanguageTest {

    @Test
    fun everyShippedLanguageRoundTripsThroughItsTag() {
        for (language in AppLanguage.selectable) {
            assertSame(
                "$language did not read back from its own tag",
                language,
                AppLanguage.fromTag(language.tag)
            )
        }
    }

    @Test
    fun tagsAreCaseInsensitiveBecauseTheyComeOffAStorageDevice() {
        assertSame(AppLanguage.GERMAN, AppLanguage.fromTag("DE"))
        assertSame(AppLanguage.GERMAN, AppLanguage.fromTag("  de  "))
        assertSame(AppLanguage.JAPANESE, AppLanguage.fromTag("Ja"))
    }

    @Test
    fun anAbsentChoiceMeansFollowTheDeviceRatherThanFollowEnglish() {
        // The two are genuinely different states and the code has to tell them
        // apart: null is "nothing stored", which is a fresh install and a player
        // who never opened Settings.
        assertNull(AppLanguage.fromTag(null))
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag("   "))
    }

    @Test
    fun aTagThisBuildDoesNotHaveFallsBackRatherThanStrandingThePlayer() {
        // A player who downgrades, or who had a language removed, must end up on
        // the device locale - not on a language that no longer has any strings.
        assertNull(AppLanguage.fromTag("sv"))
        assertNull(AppLanguage.fromTag("zh-CN"))
        assertNull(AppLanguage.fromTag("xx-YY"))
    }

    @Test
    fun aRegionalDeviceLocaleMatchesItsLanguage() {
        assertSame(AppLanguage.GERMAN, AppLanguage.fromLocale(Locale.GERMANY))
        assertSame(AppLanguage.GERMAN, AppLanguage.fromLocale(Locale.GERMAN))
        assertSame(AppLanguage.FRENCH, AppLanguage.fromLocale(Locale.FRANCE))
        assertSame(AppLanguage.SPANISH, AppLanguage.fromLocale(Locale.forLanguageTag("es-MX")))
        assertSame(AppLanguage.JAPANESE, AppLanguage.fromLocale(Locale.JAPAN))
        assertSame(AppLanguage.KOREAN, AppLanguage.fromLocale(Locale.KOREA))
    }

    @Test
    fun aBareLanguageLocaleStillMatches() {
        assertSame(AppLanguage.SPANISH, AppLanguage.fromLocale(Locale.forLanguageTag("es")))
        assertSame(AppLanguage.FRENCH, AppLanguage.fromLocale(Locale.forLanguageTag("fr")))
    }

    @Test
    fun onlyBrazilianPortugueseClaimsPortuguese() {
        // This is the one place a region qualifier is load-bearing. A European
        // Portuguese player must not be handed Brazilian wording, and must not be
        // handed a false "you are already in Portuguese" tick in the picker
        // either: null leaves the device configuration alone, and there is no
        // values-pt folder, so the resource system resolves to English.
        assertSame(
            AppLanguage.PORTUGUESE_BR,
            AppLanguage.fromLocale(Locale.forLanguageTag("pt-BR"))
        )
        assertNull(AppLanguage.fromLocale(Locale.forLanguageTag("pt-PT")))
        assertNull(AppLanguage.fromLocale(Locale.forLanguageTag("pt")))
        assertNull(AppLanguage.fromLocale(Locale.forLanguageTag("pt-AO")))
    }

    @Test
    fun anUnsupportedDeviceLocaleNamesNoLanguage() {
        // Null here is the English fallback signal, not a failure: there is no
        // values-it, so res/values wins and the player reads English.
        assertNull(AppLanguage.fromLocale(Locale.ITALIAN))
        assertNull(AppLanguage.fromLocale(Locale.forLanguageTag("zh-Hans-CN")))
        assertNull(AppLanguage.fromLocale(Locale.forLanguageTag("ar-EG")))
        assertNull(AppLanguage.fromLocale(Locale.ROOT))
        assertNull(AppLanguage.fromLocale(null))
    }

    @Test
    fun onlyTheTwoShippedCjkLanguagesNeedASystemFont() {
        val needsSystemFont = AppLanguage.selectable.filter { it.needsSystemFont }
        assertEquals(
            "The bundled fonts are Latin-only, so exactly Japanese and Korean " +
                "need the platform family",
            listOf(AppLanguage.JAPANESE, AppLanguage.KOREAN),
            needsSystemFont
        )
    }

    @Test
    fun latinLocalesKeepTheBundledFont() {
        for (language in AppLanguage.selectable) {
            if (language.needsSystemFont) continue
            assertFalse("$language should keep Poppins", language.needsSystemFont)
        }
    }

    @Test
    fun everyLanguageIsOfferedExactlyOnce() {
        assertEquals(
            "A duplicate language in the picker would show two rows that do the " +
                "same thing",
            AppLanguage.selectable.size,
            AppLanguage.selectable.map { it.tag }.toSet().size
        )
        assertEquals(7, AppLanguage.selectable.size)
    }

    @Test
    fun tagsAreDistinctAndWellFormed() {
        for (language in AppLanguage.selectable) {
            assertTrue("'${language.tag}' is not a usable BCP 47 tag", language.tag.isNotBlank())
            assertEquals(
                "'${language.tag}' should not carry a region unless it needs one",
                language.tag,
                language.tag.trim()
            )
        }
        assertEquals(
            "Two languages share a tag, so one of them can never be selected",
            AppLanguage.selectable.size,
            AppLanguage.selectable.map { it.tag.lowercase(Locale.ROOT) }.toSet().size
        )
    }

    @Test
    fun aLanguageResolvesToALocaleTheResourceSystemUnderstands() {
        assertEquals("ja", AppLanguage.JAPANESE.locale.language)
        assertEquals("ko", AppLanguage.KOREAN.locale.language)
        assertEquals("pt", AppLanguage.PORTUGUESE_BR.locale.language)
        assertEquals("BR", AppLanguage.PORTUGUESE_BR.locale.country)
        assertEquals("es", AppLanguage.SPANISH.locale.language)
        assertEquals("BR", Locale.forLanguageTag("pt-BR").country)
    }
}
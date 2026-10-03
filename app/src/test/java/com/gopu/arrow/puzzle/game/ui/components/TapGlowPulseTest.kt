package com.gopu.arrow.puzzle.game.ui.components

import com.gopu.arrow.puzzle.game.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tap glow pulse is sized entirely from the arrow it marks, so two things
 * have to hold for it: every radius is the arrow's own size times a multiplier,
 * and every layer fades to nothing inside a short, fully bounded run. These tests
 * pin both down at several arrow sizes, which is what makes the effect hold up on
 * a phone, a tablet and any board size without a single pixel value anywhere.
 */
class TapGlowPulseTest {

    private val arrowSizes = listOf(24f, 48f, 96f, 240f)

    private val config = TapGlowPulseConfig()

    /** Radii are allowed to differ from their target by this much, in pixels. */
    private val tolerance = 0.001f

    @Test
    fun radiusLayersAreTheArrowSizeTimesTheirMultiplier() {
        for (size in arrowSizes) {
            assertEquals(
                size * config.glowRadiusMultiplier,
                glowRadiusAt(size, 1f, config.glowRadiusMultiplier),
                tolerance
            )
            assertEquals(
                size * config.pulseRadiusMultiplier,
                pulseRadiusAt(size, 1f, config.pulseRadiusMultiplier),
                tolerance
            )
            assertEquals(
                size * config.ringRadiusMultiplier,
                ringRadiusAt(size, 1f, config.ringRadiusMultiplier),
                tolerance
            )
        }
    }

    @Test
    fun radiusLayersScaleLinearlyWithTheArrow() {
        for (multiplier in listOf(0.4f, 0.95f, 1.45f)) {
            val small = pulseRadiusAt(40f, 1f, multiplier)
            val large = pulseRadiusAt(80f, 1f, multiplier)
            assertEquals("doubling the arrow doubles the pulse", small * 2f, large, tolerance)
        }
    }

    @Test
    fun pulseOpensOnItsFirstFrameRatherThanFromNothing() {
        for (size in arrowSizes) {
            val opening = pulseRadiusAt(size, 0f, config.pulseRadiusMultiplier)
            val widest = pulseRadiusAt(size, 1f, config.pulseRadiusMultiplier)
            assertTrue("a pulse starting at zero reads as a late pop", opening > 0f)
            assertTrue(opening < widest)
        }
    }

    @Test
    fun glowOutlivesThePulseAndTheRingSitsInsideIt() {
        val size = 80f
        assertTrue(
            "the ambient glow has to be the wider layer",
            glowRadiusAt(size, 1f, config.glowRadiusMultiplier) >
                pulseRadiusAt(size, 1f, config.pulseRadiusMultiplier)
        )
        assertTrue(
            "the subtle ring stays inside the pulse it belongs to",
            ringRadiusAt(size, 1f, config.ringRadiusMultiplier) <
                pulseRadiusAt(size, 1f, config.pulseRadiusMultiplier)
        )
    }

    @Test
    fun everyLayerOnlyEverGrows() {
        for (size in arrowSizes) {
            var previousGlow = 0f
            var previousPulse = 0f
            var previousRing = 0f
            for (step in 0..20) {
                val t = step / 20f
                val glow = glowRadiusAt(size, t, config.glowRadiusMultiplier)
                val pulse = pulseRadiusAt(size, t, config.pulseRadiusMultiplier)
                val ring = ringRadiusAt(size, t, config.ringRadiusMultiplier)
                assertTrue(glow >= previousGlow)
                assertTrue(pulse >= previousPulse)
                assertTrue(ring >= previousRing)
                previousGlow = glow
                previousPulse = pulse
                previousRing = ring
            }
        }
    }

    @Test
    fun ringThinsAsItTravelsAndNeverVanishes() {
        for (size in arrowSizes) {
            val start = ringThicknessAt(size, 0f, config.ringThicknessMultiplier)
            val end = ringThicknessAt(size, 1f, config.ringThicknessMultiplier)
            assertEquals(size * config.ringThicknessMultiplier, start, tolerance)
            assertTrue("a wave thins as it expands", end < start)
            assertTrue("but stays drawable on the smallest arrow", end > 0f)
        }
        assertEquals(
            "the floor keeps the ring drawable on a very small arrow",
            2f * 0.012f,
            ringThicknessAt(2f, 1f, 0.001f),
            tolerance
        )
    }

    @Test
    fun hotCentreIsFullSizeOnTheTapFrameAndGoneByTheEnd() {
        for (size in arrowSizes) {
            assertEquals(
                size * config.coreRadiusMultiplier,
                coreRadiusAt(size, 0f, config.coreRadiusMultiplier),
                tolerance
            )
            assertEquals(0f, coreRadiusAt(size, 1f, config.coreRadiusMultiplier), tolerance)
            assertTrue(
                "instant attack: the centre is already gone a third of the way in",
                coreRadiusAt(size, 0.33f, config.coreRadiusMultiplier) <
                    size * config.coreRadiusMultiplier * 0.5f
            )
        }
    }

    @Test
    fun alphaIsFullOnTheFirstFrameAndSpentAtTheEnd() {
        for (size in arrowSizes) {
            assertEquals(config.glowIntensity, pulseAlphaAt(0f, config.glowIntensity, 1f), tolerance)
            assertEquals(0f, pulseAlphaAt(1f, config.glowIntensity, 1f), tolerance)
        }
    }

    @Test
    fun fadeAmountDecidesHowMuchAlphaIsLeft() {
        assertEquals(0.5f, pulseAlphaAt(1f, 1f, 0.5f), tolerance)
        assertEquals(1f, pulseAlphaAt(1f, 1f, 0f), tolerance)
    }

    @Test
    fun alphaNeverLeavesItsBoundsWhateverIsConfigured() {
        for (intensity in listOf(0f, 0.5f, 4f)) {
            for (fade in listOf(-1f, 0f, 0.5f, 1f, 3f)) {
                for (t in listOf(-1f, 0f, 0.5f, 1f, 2f)) {
                    val alpha = pulseAlphaAt(t, intensity, fade)
                    assertTrue(alpha in 0f..1f)
                }
            }
        }
    }

    @Test
    fun arrowBrightensOnTheTapFrameAndIsNeverLiftedOutsideAPulse() {
        for (boost in listOf(0f, 0.34f, 0.9f)) {
            assertEquals(boost, arrowGlowBoostAt(0f, boost), tolerance)
            assertEquals(0f, arrowGlowBoostAt(1f, boost), tolerance)
            assertTrue(arrowGlowBoostAt(0.2f, boost) >= arrowGlowBoostAt(0.8f, boost))
        }
        assertEquals("no pulse means no lift at all", 0f, arrowGlowBoostAt(0f, 0f), tolerance)
    }

    @Test
    fun progressOutsideTheRunIsClampedNotExtrapolated() {
        assertEquals(pulseRadiusAt(80f, 0f, 0.9f), pulseRadiusAt(80f, -4f, 0.9f), tolerance)
        assertEquals(pulseRadiusAt(80f, 1f, 0.9f), pulseRadiusAt(80f, 9f, 0.9f), tolerance)
        assertEquals(0f, coreRadiusAt(80f, 3f, 0.3f), tolerance)
        assertEquals(
            "a pulse asked for before its first frame is simply at its first frame",
            0.8f,
            arrowGlowBoostAt(-2f, 0.8f),
            tolerance
        )
    }

    @Test
    fun thePulseIsShortEnoughToNeverHoldUpGameplay() {
        assertTrue(
            "a launch animation that stalls a tap is a launch animation nobody wants",
            config.durationMillis in 80..120
        )
        assertEquals(110, DefaultDurationMillis)
        assertEquals(config.durationMillis, TapGlowPulseConfig().durationMillis)
    }

    @Test
    fun everyDefaultIsARelativeSizeAndNeverAPixelCount() {
        assertTrue(config.pulseRadiusMultiplier in 0.3f..2f)
        assertTrue(config.glowRadiusMultiplier in 0.3f..3f)
        assertTrue(config.ringRadiusMultiplier in 0.2f..2f)
        assertTrue(config.ringThicknessMultiplier in 0.01f..0.2f)
        assertTrue(config.coreRadiusMultiplier in 0.1f..1f)
        assertTrue(config.glowIntensity in 0.1f..1f)
        assertTrue(config.ringIntensity in 0.1f..1f)
        assertTrue(config.arrowGlowBoost in 0f..1f)
        assertTrue(config.fadeAmount in 0f..1f)
    }

    @Test
    fun styleDefaultsToTheGlowPulseSoTheEffectIsAlreadyThere() {
        assertSame(LaunchAnimationStyle.GLOW_PULSE, LaunchAnimationStyle.Default)
        assertTrue(LaunchAnimationStyle.Default.animatesTap)
    }

    @Test
    fun theDefaultStyleIsThePlainGlowPulse() {
        assertEquals(
            "an unconfigured default has to keep meaning the documented one",
            TapGlowPulseConfig(),
            launchProfile(LaunchAnimationStyle.GLOW_PULSE).pulse
        )
        assertNull(launchProfile(LaunchAnimationStyle.Default).burst)
    }

    @Test
    fun onlyOffStaysSilent() {
        assertEquals(
            listOf(LaunchAnimationStyle.OFF),
            LaunchAnimationStyle.options.filterNot { it.animatesTap }
        )
        assertFalse(LaunchAnimationStyle.OFF.animatesTap)
    }

    @Test
    fun theFullOptionSetIsOfferedInOrder() {
        /*
         * Resource ids rather than the rendered words: the label is a
         * `@ReadOnlyComposable` getter now, because the settings screen is
         * translated. What this pins is the set and the order, which is the part
         * the picker and the persisted style names depend on; that each id
         * resolves to the expected English wording is
         * `L10nStringsTest`'s job, since only a real context can do it.
         */
        assertEquals(
            listOf(
                R.string.launch_style_off,
                R.string.launch_style_minimal,
                R.string.launch_style_glow_pulse,
                R.string.launch_style_glow_energy,
                R.string.launch_style_full
            ),
            LaunchAnimationStyle.options.map { it.labelRes }
        )
    }

    @Test
    fun everyOfferedStyleIsBuilt() {
        for (style in LaunchAnimationStyle.options) {
            assertTrue("$style is offered in settings but not built", style.implemented)
        }
    }

    @Test
    fun aStoredStyleRoundTripsAndUnknownValuesFallBackToTheDefault() {
        for (style in LaunchAnimationStyle.options) {
            assertSame(style, LaunchAnimationStyle.fromName(style.name))
        }
        assertSame(LaunchAnimationStyle.Default, LaunchAnimationStyle.fromName(null))
        assertSame(LaunchAnimationStyle.Default, LaunchAnimationStyle.fromName(""))
        assertSame(
            LaunchAnimationStyle.Default,
            LaunchAnimationStyle.fromName("removed_in_a_future_release")
        )
        assertSame(
            "stored names are matched exactly, so a mistyped value falls back to the default",
            LaunchAnimationStyle.Default,
            LaunchAnimationStyle.fromName("glow pulse")
        )
    }
}
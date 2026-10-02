package com.gopu.arrow.puzzle.game.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * The energy burst is the second half of the richer launch styles, and it shares
 * the glow pulse's one hard rule: nothing is measured in pixels, everything is
 * the tapped arrow's own size times a multiplier. These tests pin that down, and
 * pin the scatter, because a burst that is perfectly symmetrical or that only
 * holds together at one board size is not the effect the setting promises.
 */
class TapEnergyBurstTest {

    private val arrowSizes = listOf(24f, 48f, 96f, 240f)

    private val config = TapEnergyBurstConfig()

    private val tolerance = 0.0001f

    @Test
    fun sparkReachIsTheArrowSizeTimesItsMultiplier() {
        for (size in arrowSizes) {
            val reach = particleReachAt(size, 0, 1f, config.particleReachMultiplier)
            assertTrue(reach in 0f..(size * config.particleReachMultiplier))
            assertTrue(reach > 0f)
        }
    }

    @Test
    fun sparkGeometryScalesLinearlyWithTheArrow() {
        for (index in 0 until config.particleCount) {
            val small = particleReachAt(40f, index, 1f, config.particleReachMultiplier)
            val large = particleReachAt(80f, index, 1f, config.particleReachMultiplier)
            assertEquals("doubling the arrow doubles every spark", small * 2f, large, small * 0.001f)
        }
        assertEquals(
            particleLengthAt(40f, 0.5f, config.particleTrailMultiplier) * 2f,
            particleLengthAt(80f, 0.5f, config.particleTrailMultiplier),
            tolerance
        )
        assertEquals(
            particleStrokeScaleAt(0.5f) * config.particleRadiusMultiplier * 40f * 2f,
            particleStrokeScaleAt(0.5f) * config.particleRadiusMultiplier * 80f,
            tolerance
        )
    }

    @Test
    fun everySparkStaysWithinItsOwnReach() {
        for (size in arrowSizes) {
            for (index in 0 until config.particleCount) {
                val reach = size * config.particleReachMultiplier
                for (step in 0..20) {
                    val at = particleReachAt(size, index, step / 20f, config.particleReachMultiplier)
                    assertTrue(at in 0f..reach)
                }
            }
        }
    }

    @Test
    fun sparksFlyOutwardAndOnlyOutward() {
        for (index in 0 until config.particleCount) {
            var previous = 0f
            for (step in 0..20) {
                val reach = particleReachAt(96f, index, step / 20f, config.particleReachMultiplier)
                assertTrue("a spark must never fall back toward the arrow", reach >= previous)
                previous = reach
            }
        }
    }

    @Test
    fun theScatterIsNotAStarburst() {
        val even = (0 until config.particleCount).map { TWO_PI_ * it / config.particleCount }
        for (index in 0 until config.particleCount) {
            val offset = abs(angleWrap(particleAngleAt(index, config.particleCount) - even[index]))
            assertTrue(
                "spark $index sits exactly on its even angle, which reads as a starburst",
                offset > 0.01f
            )
        }
    }

    @Test
    fun theScatterIsStableAndSpreadOut() {
        val angles = (0 until config.particleCount).map { particleAngleAt(it, config.particleCount) }
        assertEquals("the same spark must leave the same way every frame", angles, angles.toList())
        for (a in angles.indices) {
            for (b in a + 1 until angles.size) {
                assertNotEquals("two sparks sharing a direction reads as one thick mark", angles[a], angles[b])
            }
        }
    }

    @Test
    fun sparkSpeedAndWeightStayInsideTheirStatedSpread() {
        for (index in 0 until config.particleCount) {
            assertTrue(particleSpeedAt(index) in 0.72f..1f)
            assertTrue(particleWeightAt(index) in 0.6f..1f)
        }
        assertTrue(
            "at least one spark has to be slower than another, or the burst is a solid ring",
            (0 until config.particleCount).map { particleSpeedAt(it) }.distinct().size > 1
        )
    }

    @Test
    fun sparksAreBrightOnTheTapFrameAndGoneByTheEnd() {
        for (index in 0 until config.particleCount) {
            assertTrue(particleAlphaAt(index, 0f, config.particleIntensity, 1f) > 0f)
            assertEquals(0f, particleAlphaAt(index, 1f, config.particleIntensity, 1f), tolerance)
        }
    }

    @Test
    fun sparkAlphaNeverLeavesItsBoundsWhateverIsConfigured() {
        for (index in 0 until config.particleCount) {
            for (intensity in listOf(0f, 0.5f, 9f)) {
                for (fade in listOf(-1f, 0f, 1f, 4f)) {
                    for (t in listOf(-1f, 0f, 0.5f, 1f, 3f)) {
                        assertTrue(particleAlphaAt(index, t, intensity, fade) in 0f..1f)
                    }
                }
            }
        }
    }

    @Test
    fun streaksShortenAndThinAsTheyTravel() {
        for (size in arrowSizes) {
            assertTrue(
                particleLengthAt(size, 1f, config.particleTrailMultiplier) <
                    particleLengthAt(size, 0f, config.particleTrailMultiplier)
            )
            assertEquals(1f, particleStrokeScaleAt(0f), tolerance)
            assertTrue(particleStrokeScaleAt(1f) < particleStrokeScaleAt(0f))
            assertTrue(particleLengthAt(size, 2f, config.particleTrailMultiplier) >= 0f)
        }
    }

    @Test
    fun energyRingsTrailEachOtherInsteadOfStacking() {
        for (size in arrowSizes) {
            val lead = burstRingRadiusAt(size, 0, 1f, config.ringRadiusMultiplier, config.ringLag)
            val follow = burstRingRadiusAt(size, 1, 1f, config.ringRadiusMultiplier, config.ringLag)
            assertEquals(size * config.ringRadiusMultiplier, lead, size * 0.001f)
            assertTrue("a trailing ring has to be behind the one ahead of it", follow < lead)
            assertTrue("and still inside the widest reach of the arrow", follow > 0f)
        }
    }

    @Test
    fun aRingOnlyExistsOnceItsTurnHasCome() {
        assertEquals(0f, burstRingProgressAt(1, 0.2f, 0.32f), tolerance)
        assertEquals(0.2f, burstRingProgressAt(0, 0.2f, 0.32f), tolerance)
        assertTrue(burstRingProgressAt(1, 0.9f, 0.32f) in 0.5f..0.6f)
        assertEquals("a negative lag would run the trail backwards", 0.5f, burstRingProgressAt(1, 0.5f, -1f), tolerance)
    }

    @Test
    fun progressOutsideTheRunIsClampedNotExtrapolated() {
        assertEquals(0f, particleTravelAt(-3f), tolerance)
        assertEquals(1f, particleTravelAt(7f), tolerance)
        assertEquals(0f, particleReachAt(80f, 0, -3f, 1f), tolerance)
        assertEquals(
            particleReachAt(80f, 0, 1f, 1f),
            particleReachAt(80f, 0, 7f, 1f),
            tolerance
        )
    }

    @Test
    fun aStyleWithNoSparksOrRingsAsksForNone() {
        val silent = TapEnergyBurstConfig(
            particleCount = 0,
            ringCount = 0,
            particleIntensity = 0f,
            particleHeadIntensity = 0f,
            ringIntensity = 0f
        )
        assertEquals(0, silent.particleCount)
        assertEquals(0, silent.ringCount)
    }

    @Test
    fun aSingleSparkStillHasAWellDefinedDirection() {
        val angle = particleAngleAt(0, 1)
        assertEquals(
            "the same spark must leave the same way every frame",
            angle,
            particleAngleAt(0, 1),
            tolerance
        )
        assertTrue("and with no even angle to sit on, it still has to leave somewhere", angle != 0f)
        assertTrue("inside the scatter the effect allows", abs(angle) <= 0.45f + tolerance)
    }

    private fun angleWrap(angle: Float): Float {
        var wrapped = angle
        while (wrapped > PI) wrapped -= (2 * PI).toFloat()
        while (wrapped < -PI) wrapped += (2 * PI).toFloat()
        return wrapped
    }

    private companion object {
        const val TWO_PI_ = 6.2831855f
    }
}

/**
 * The style setting has to reach the board, and the only part of that chain a
 * unit test can hold is the profile table: every style has to resolve to a real
 * look, the styles have to actually differ from each other, and none of them may
 * quietly do nothing or drift outside the short run the gameplay budget allows.
 */
class LaunchAnimationProfileTest {

    private val tolerance = 0.0001f

    private val profiles = LaunchAnimationStyle.options.associateWith { launchProfile(it) }

    @Test
    fun everyStyleResolvesToItsOwnProfile() {
        for (style in LaunchAnimationStyle.options) {
            assertSame(style, profiles.getValue(style).style)
        }
        for (style in LaunchAnimationStyle.options) {
            for (other in LaunchAnimationStyle.options) {
                if (style == other) continue
                assertNotSame(
                    "two styles share one profile",
                    profiles.getValue(style),
                    profiles.getValue(other)
                )
            }
        }
    }

    @Test
    fun everyStyleIsImplementedAndOnlyOffStaysSilent() {
        for (style in LaunchAnimationStyle.options) {
            assertTrue("$style is offered in settings but not built", style.implemented)
        }
        assertEquals(
            listOf(LaunchAnimationStyle.OFF),
            LaunchAnimationStyle.options.filterNot { it.animatesTap }
        )
    }

    @Test
    fun everyPlayingStyleHasAGlowAndStaysResponsive() {
        for (style in LaunchAnimationStyle.options.filter { it.animatesTap }) {
            val pulse = profiles.getValue(style).pulse
            assertTrue("$style plays no glow at all", pulse.glowIntensity > 0f)
            assertTrue("$style runs for ${pulse.durationMillis}ms", pulse.durationMillis in 80..120)
        }
    }

    @Test
    fun offPlaysNothingAtAll() {
        val profile = profiles.getValue(LaunchAnimationStyle.OFF)
        assertNull(profile.burst)
        assertEquals(0f, profile.pulse.glowIntensity, tolerance)
        assertEquals(0f, profile.pulse.ringIntensity, tolerance)
        assertEquals(0f, profile.pulse.coreIntensity, tolerance)
        assertEquals(0f, profile.pulse.arrowGlowBoost, tolerance)
    }

    @Test
    fun minimalIsTheQuietestStyleThatStillReads() {
        val minimal = profiles.getValue(LaunchAnimationStyle.MINIMAL)
        assertNull(minimal.burst)
        assertEquals("minimal drops the ring entirely", 0f, minimal.pulse.ringIntensity, tolerance)
        assertTrue(minimal.durationMillis in 80..100)
        for (other in listOf(LaunchAnimationStyle.GLOW_PULSE, LaunchAnimationStyle.GLOW_ENERGY, LaunchAnimationStyle.FULL)) {
            val pulse = profiles.getValue(other).pulse
            assertTrue("minimal must stay smaller than $other", minimal.pulse.glowRadiusMultiplier < pulse.glowRadiusMultiplier)
            assertTrue("minimal must stay dimmer than $other", minimal.pulse.glowIntensity < pulse.glowIntensity)
        }
    }

    @Test
    fun glowPulseIsTheGlowAloneAndEnergyAddsSparks() {
        assertNull(profiles.getValue(LaunchAnimationStyle.GLOW_PULSE).burst)
        val energy = profiles.getValue(LaunchAnimationStyle.GLOW_ENERGY).burst
        assertTrue("Glow + Energy throws sparks", energy!!.particleCount > 0)
        assertTrue("and runs a shock ring", energy.ringIntensity > 0f)
        assertTrue("with a bright head on each spark", energy.particleHeadIntensity > 0f)
    }

    @Test
    fun fullIsTheMostElaborateStyle() {
        val full = profiles.getValue(LaunchAnimationStyle.FULL)
        val energy = full.burst!!
        val glowPlusEnergy = profiles.getValue(LaunchAnimationStyle.GLOW_ENERGY).burst!!
        assertTrue("Full throws more sparks", energy.particleCount > glowPlusEnergy.particleCount)
        assertTrue("and trails more than one ring", energy.ringCount > glowPlusEnergy.ringCount)
        assertTrue("and reaches further", energy.particleReachMultiplier > glowPlusEnergy.particleReachMultiplier)
        assertTrue(
            "and glows widest",
            full.pulse.glowRadiusMultiplier > profiles.getValue(LaunchAnimationStyle.GLOW_ENERGY).pulse.glowRadiusMultiplier
        )
        assertEquals(120, full.durationMillis)
    }

    @Test
    fun theStylesGetLouderAndWiderInStepWithTheMenu() {
        val ordered = listOf(
            LaunchAnimationStyle.MINIMAL,
            LaunchAnimationStyle.GLOW_PULSE,
            LaunchAnimationStyle.GLOW_ENERGY,
            LaunchAnimationStyle.FULL
        )
        for ((index, style) in ordered.withIndex()) {
            val pulse = profiles.getValue(style).pulse
            if (index == 0) continue
            val previous = profiles.getValue(ordered[index - 1]).pulse
            assertTrue("$style must not be dimmer than the style above it", pulse.glowIntensity >= previous.glowIntensity)
            assertTrue("$style must not lift the arrow less", pulse.arrowGlowBoost >= previous.arrowGlowBoost)
        }
    }

    @Test
    fun everySizeIsARealRelativeSizeAndNeverAPixelCount() {
        for (style in LaunchAnimationStyle.options) {
            val pulse = profiles.getValue(style).pulse
            assertTrue("$style pulse radius", pulse.pulseRadiusMultiplier in 0f..2f)
            assertTrue("$style glow radius", pulse.glowRadiusMultiplier in 0f..3f)
            assertTrue("$style ring radius", pulse.ringRadiusMultiplier in 0f..2f)
            assertTrue("$style ring thickness", pulse.ringThicknessMultiplier in 0f..0.2f)
            assertTrue("$style core radius", pulse.coreRadiusMultiplier in 0f..1f)
            assertTrue("$style glow alpha", pulse.glowIntensity in 0f..1f)
            assertTrue("$style ring alpha", pulse.ringIntensity in 0f..1f)
            assertTrue("$style arrow lift", pulse.arrowGlowBoost in 0f..1f)
            assertTrue("$style fade", pulse.fadeAmount in 0f..1f)

            val burst = profiles.getValue(style).burst ?: continue
            assertTrue("$style spark count", burst.particleCount in 0..24)
            assertTrue("$style spark thickness", burst.particleRadiusMultiplier in 0f..0.2f)
            assertTrue("$style spark reach", burst.particleReachMultiplier in 0f..3f)
            assertTrue("$style spark streak", burst.particleTrailMultiplier in 0f..2f)
            assertTrue("$style spark alpha", burst.particleIntensity in 0f..1f)
            assertTrue("$style energy ring radius", burst.ringRadiusMultiplier in 0f..3f)
            assertTrue("$style energy ring thickness", burst.ringThicknessMultiplier in 0f..0.2f)
            assertTrue("$style energy ring alpha", burst.ringIntensity in 0f..1f)
            assertTrue("$style ring count", burst.ringCount in 0..4)
            assertTrue("$style ring lag", burst.ringLag in 0f..1f)
            assertTrue("$style fade", burst.fadeAmount in 0f..1f)
        }
    }

    @Test
    fun aTrailingRingNeverInvertsIntoALeadingOne() {
        for (style in LaunchAnimationStyle.options) {
            val burst = profiles.getValue(style).burst ?: continue
            assertTrue("$style rings must not run backwards", burst.ringLag >= 0f)
        }
    }

    @Test
    fun aStylesRunIsTimedOnceSoItsHalvesCannotDrift() {
        for (style in LaunchAnimationStyle.options) {
            val profile = profiles.getValue(style)
            assertEquals(profile.pulse.durationMillis, profile.durationMillis)
        }
    }

    @Test
    fun everyStyleStillRoundTripsThroughStorage() {
        for (style in LaunchAnimationStyle.options) {
            assertSame(style, LaunchAnimationStyle.fromName(style.name))
        }
        assertSame(LaunchAnimationStyle.Default, LaunchAnimationStyle.fromName(null))
        assertSame(LaunchAnimationStyle.GLOW_PULSE, LaunchAnimationStyle.Default)
        assertEquals(
            listOf("Off", "Minimal", "Glow Pulse", "Glow + Energy", "Full"),
            LaunchAnimationStyle.options.map { it.label }
        )
    }
}
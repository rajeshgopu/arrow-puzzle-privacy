package com.gopu.arrow.puzzle.game.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The launch is one animation split across two layers, so every guarantee that
 * makes the handoff invisible lives in the curves in [SplashTransition.kt] and
 * nowhere else. These tests pin those guarantees without a device: nothing pops
 * at either end, the two layers hand over instead of cutting, and the field the
 * brand sits on is the field the menu ends on.
 */
class SplashTransitionTest {

    /** Reveal sampled across the whole handoff, at a typical frame interval. */
    private val frames = 40

    @Test
    fun revealIsAFrameInsideTheRequestedTransitionWindow() {
        assertTrue(
            "handoff is ${SplashRevealMillis}ms, outside the 250-400ms window",
            SplashRevealMillis in 250..400
        )
    }

    @Test
    fun brandHoldsBeforeTheHandoffStarts() {
        assertTrue("brand holds for ${SplashHoldMillis}ms", SplashHoldMillis > 0L)
        assertTrue(
            "intro of ${SplashIntroMillis}ms outlasts the brand hold",
            SplashIntroMillis < SplashHoldMillis
        )
    }

    @Test
    fun fieldIsPaintedUpFrontAndGoneAtTheEnd() {
        assertEquals(1f, splashFieldAlphaAt(0f), Tolerance)
        assertEquals(0f, splashFieldAlphaAt(1f), Tolerance)

        // Nothing before the reveal may hide the field: it is what covers the
        // window during the very first frames.
        for (step in 0..frames) {
            val reveal = -1f + step / frames.toFloat()
            assertEquals(
                "field at reveal $reveal",
                1f,
                splashFieldAlphaAt(reveal),
                Tolerance
            )
        }
    }

    @Test
    fun fieldOnlyEverFadesOut() {
        var previous = splashFieldAlphaAt(0f)
        for (step in 1..frames) {
            val alpha = splashFieldAlphaAt(step / frames.toFloat())
            assertTrue("field brightened at step $step", alpha <= previous)
            previous = alpha
        }
    }

    @Test
    fun markIsInvisibleUntilTheIntroHasRunAndGoneByMidHandoff() {
        assertEquals(0f, splashMarkAlphaAt(0f, 0f), Tolerance)
        assertEquals(1f, splashMarkAlphaAt(1f, 0f), Tolerance)

        for (step in 0..frames) {
            val reveal = step / frames.toFloat()
            if (reveal >= 0.5f) {
                assertEquals(
                    "mark still visible at reveal $reveal",
                    0f,
                    splashMarkAlphaAt(1f, reveal),
                    Tolerance
                )
            }
        }
    }

    @Test
    fun markSettlesOutOfTheIntroThenLeavesAlongTheHandoffDrift() {
        assertTrue("mark never starts oversized", splashMarkScaleAt(0f, 0f) > 1f)
        assertEquals(1f, splashMarkScaleAt(1f, 0f), Tolerance)
        assertTrue("mark does not leave", splashMarkScaleAt(1f, 1f) > 1f)
    }

    @Test
    fun scaleStaysInTheRangeTheLayoutCanAbsorb() {
        // The intro always settles before the handoff starts, so the mark only
        // ever travels one scale axis at a time.
        for (step in 0..frames) {
            val introScale = splashMarkScaleAt(step / frames.toFloat(), 0f)
            assertTrue("intro scale $introScale", introScale in 1f..1.1f)

            val outScale = splashMarkScaleAt(1f, step / frames.toFloat())
            assertTrue("handoff scale $outScale", outScale in 1f..1.1f)

            val homeScale = homeScaleAt(step / frames.toFloat())
            assertTrue("home scale $homeScale", homeScale in 1f..1.05f)
        }
    }

    @Test
    fun menuIsInvisibleDuringTheHoldAndFinishedAtTheEnd() {
        assertEquals(0f, homeAlphaAt(0f), Tolerance)
        assertEquals(1f, homeAlphaAt(1f), Tolerance)
        assertEquals(1f, homeScaleAt(1f), Tolerance)

        for (step in 1..frames) {
            val reveal = step / frames.toFloat()
            if (reveal < 0.1f) {
                assertEquals(
                    "menu visible during the hold at reveal $reveal",
                    0f,
                    homeAlphaAt(reveal),
                    Tolerance
                )
            }
        }
    }

    @Test
    fun menuOnlyEverFadesIn() {
        var previous = homeAlphaAt(0f)
        for (step in 1..frames) {
            val alpha = homeAlphaAt(step / frames.toFloat())
            assertTrue("menu dimmed at step $step", alpha >= previous)
            previous = alpha
        }
    }

    @Test
    fun handoffAlwaysHasOneVisibleLayer() {
        for (step in 0..frames) {
            val reveal = step / frames.toFloat()
            val field = splashFieldAlphaAt(reveal)
            assertTrue(
                "dead frame at reveal $reveal: splash gone, menu not up yet",
                field > FaintestLayer || homeAlphaAt(reveal) >= Halfway
            )
        }
    }

    @Test
    fun menuIsFinishedBeforeTheSplashLeavesSoNothingPopsOnUnmount() {
        val revealMenuIsOpaque = (0..frames)
            .map { it / frames.toFloat() }
            .first { homeAlphaAt(it) >= 1f - Tolerance }

        assertTrue(
            "menu is still fading at the end of the handoff",
            revealMenuIsOpaque < 1f
        )
        assertTrue(
            "field is still on screen once the menu is opaque",
            splashFieldAlphaAt(revealMenuIsOpaque) <= 0.2f
        )
        assertEquals(
            "field survives the end of the handoff",
            0f,
            splashFieldAlphaAt(1f),
            Tolerance
        )
        assertEquals(
            "mark survives the end of the handoff",
            0f,
            splashMarkAlphaAt(1f, 1f),
            Tolerance
        )
    }

    @Test
    fun menuArrivesOversizedSoItsEdgesCanOnlyBeClipped() {
        assertTrue("menu does not arrive oversized", homeScaleAt(0.1f) > 1f)

        for (step in 0..frames) {
            assertTrue(
                "menu is ever inset at step $step",
                homeScaleAt(step / frames.toFloat()) >= 1f
            )
        }
    }

    private companion object {
        const val Tolerance = 1e-4f

        /** Below this the splash field has all but gone. */
        const val FaintestLayer = 0.05f

        /** The menu this far up reads as arrived, not as arriving. */
        const val Halfway = 0.5f
    }
}
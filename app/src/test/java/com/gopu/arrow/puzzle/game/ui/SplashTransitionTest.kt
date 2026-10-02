package com.gopu.arrow.puzzle.game.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The launch is one animation split across two layers, and the scene on top of
 * it is a dozen pieces each with its own entrance, so every guarantee that makes
 * the launch seamless lives in the curves in [SplashTransition.kt] and nowhere
 * else. These tests pin those guarantees without a device: nothing pops at either
 * end, the two layers hand over instead of cutting, the field the scene stands on
 * is the field the menu ends on, and no piece is still moving when the handoff
 * begins.
 */
class SplashTransitionTest {

    /** Every clock sampled across its whole window, at a typical frame interval. */
    private val frames = 40

    /** How much one piece is allowed to overshoot on its way in. */
    private val pop = 0.14f

    /** Every start in the launch, in the order the scene assembles itself. */
    private val starts = listOf(SplashArrowStart) +
        SplashCubeStarts +
        listOf(SplashGemStart, SplashPlaqueStart, SplashLoaderStart)

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
    fun sceneIsInvisibleUntilTheIntroHasRunAndGoneByMidHandoff() {
        assertEquals(0f, splashMarkAlphaAt(0f, 0f), Tolerance)
        assertEquals(1f, splashMarkAlphaAt(1f, 0f), Tolerance)

        for (step in 0..frames) {
            val reveal = step / frames.toFloat()
            if (reveal >= 0.5f) {
                assertEquals(
                    "scene still visible at reveal $reveal",
                    0f,
                    splashMarkAlphaAt(1f, reveal),
                    Tolerance
                )
            }
        }
    }

    @Test
    fun sceneSettlesOutOfTheIntroThenLeavesAlongTheHandoffDrift() {
        assertTrue("scene never starts oversized", splashMarkScaleAt(0f, 0f) > 1f)
        assertEquals(1f, splashMarkScaleAt(1f, 0f), Tolerance)
        assertTrue("scene does not leave", splashMarkScaleAt(1f, 1f) > 1f)
    }

    @Test
    fun sceneScaleStaysInTheRangeTheLayoutCanAbsorb() {
        // The intro always settles before the handoff starts, so the scene only
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
    fun theArrowLeadsTheSceneAndNothingElseDoes() {
        assertEquals(0f, SplashArrowStart, Tolerance)
        for (start in starts) {
            assertTrue("a piece starts before the arrow at $start", start >= SplashArrowStart)
        }
    }

    @Test
    fun theSceneAssemblesInOrder() {
        for (index in 0 until starts.lastIndex) {
            assertTrue(
                "piece ${index + 1} starts at ${starts[index + 1]} before piece $index",
                starts[index + 1] > starts[index]
            )
        }
    }

    @Test
    fun everyPieceHasLandedByTheEndOfTheIntro() {
        // The hold begins once the intro is over, so a piece still travelling at
        // intro = 1 would be moving while the brand is supposed to be at rest.
        for (start in starts) {
            assertEquals(
                "piece starting at $start has not landed",
                1f,
                splashPieceAlphaAt(1f, start),
                Tolerance
            )
            assertEquals(
                "piece starting at $start is still travelling",
                0f,
                splashPieceTravelAt(1f, start),
                Tolerance
            )
            assertEquals(
                "piece starting at $start is still turned",
                0f,
                splashPieceTurnAt(1f, start, 18f),
                Tolerance
            )
        }
    }

    @Test
    fun noPieceIsEverHalfVisibleBeforeItsCue() {
        for (start in starts) {
            val alpha = splashPieceAlphaAt(start - 0.01f, start)
            assertEquals("piece starting at $start shows early", 0f, alpha, Tolerance)
            assertEquals(
                "piece starting at $start has already travelled",
                1f,
                splashPieceTravelAt(start - 0.01f, start),
                Tolerance
            )
        }
    }

    @Test
    fun piecesOnlyEverFadeIn() {
        for (start in starts) {
            var previous = 0f
            for (step in 0..frames) {
                val intro = start - 0.05f + step / frames.toFloat() * (SplashPieceSpan + 0.10f)
                val alpha = splashPieceAlphaAt(intro, start)
                assertTrue("piece starting at $start dimmed at intro $intro", alpha >= previous)
                previous = alpha
            }
        }
    }

    @Test
    fun piecesOnlyEverSettleDownOnTheirFinalScale() {
        for (start in starts) {
            var settled = false
            var previous = splashPieceScaleAt(start, start, From, pop)
            for (step in 1..frames) {
                val intro = start + (1f - start) * step / frames.toFloat()
                val scale = splashPieceScaleAt(intro, start, From, pop)
                if (settled) {
                    assertTrue(
                        "piece starting at $start rose again after its peak",
                        scale <= previous + Tolerance
                    )
                } else if (scale < previous) {
                    settled = true
                }
                assertTrue(
                    "piece starting at $start left its band at $scale",
                    scale in From - Tolerance..1f + pop
                )
                previous = scale
            }
        }
    }

    @Test
    fun aPieceStartsSmallAndComesToRestOnExactlyOne() {
        for (start in starts) {
            assertEquals(
                "piece starting at $start does not arrive small",
                From,
                splashPieceScaleAt(start, start, From, pop),
                Tolerance
            )
            assertEquals(
                "piece starting at $start does not rest on one",
                1f,
                splashPieceScaleAt(1f, start, From, pop),
                Tolerance
            )
        }
    }

    @Test
    fun aPieceOvershootsOnTheWayIn() {
        var overshot = false
        for (start in starts) {
            for (step in 0..frames) {
                val intro = start + (1f - start) * step / frames.toFloat()
                if (splashPieceScaleAt(intro, start, From, pop) > 1f + Tolerance) {
                    overshot = true
                }
            }
        }
        assertTrue("no piece ever sails past its resting size", overshot)
    }

    @Test
    fun aPieceWithoutAnOvershootBudgetNeverLeavesOne() {
        val start = SplashLoaderStart
        for (step in 0..frames) {
            val intro = start + (1f - start) * step / frames.toFloat()
            val scale = splashPieceScaleAt(intro, start, From, pop = 0f)
            assertTrue("loader scale $scale is not a plain arrival", scale <= 1f + Tolerance)
        }
    }

    @Test
    fun aPieceTurnsOnlyTowardItsRestingAngle() {
        for (start in starts) {
            var previousTurn = Float.MAX_VALUE
            var previousTravel = Float.MAX_VALUE
            for (step in 0..frames) {
                val intro = start - 0.05f + step / frames.toFloat() * (SplashPieceSpan + 0.10f)
                val travel = splashPieceTravelAt(intro, start)
                val turn = splashPieceTurnAt(intro, start, 18f)
                assertTrue("piece starting at $start spun away from its angle", turn in 0f..18f)
                assertTrue("piece starting at $start over-travelled", travel in 0f..1f)
                assertTrue("piece starting at $start reversed its travel", travel <= previousTravel)
                assertTrue("piece starting at $start turned backwards", turn <= previousTurn)
                previousTravel = travel
                previousTurn = turn
            }
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
            "scene survives the end of the handoff",
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

        /** The size every piece of the launch arrives from. */
        const val From = 0.55f
    }
}

package com.gopu.arrow.puzzle.game.ui.components

import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * The level clear celebration is all timeline: everything on screen is a pure
 * function of one clock, which is exactly what makes it scrubbable and cheap.
 *
 * These tests pin down that timeline. Two things matter most. The shape of every
 * curve, because that is the feel of the effect and it is easy to break with an
 * innocuous edit. And the budget, because a burst or a ribbon that outlives the
 * celebration would be cut off by the result card appearing underneath it.
 */
class StarClearCelebrationTest {

    private val width = 1080f
    private val height = 1920f

    private fun model(stars: Int) = CelebrationModel.build(width, height, stars)

    /* ---------------------------------------------------------------- tiers */

    @Test
    fun starCountPicksItsTier() {
        assertEquals(ClearTier.SIMPLE, ClearTier.of(1))
        assertEquals(ClearTier.GOOD, ClearTier.of(2))
        assertEquals(ClearTier.PERFECT, ClearTier.of(3))
    }

    @Test
    fun everyTierKeepsTheCelebrationUnderThreeSeconds() {
        val budgets = listOf(1 to 1700, 2 to 2400, 3 to 3000)
        for ((stars, millis) in budgets) {
            val tier = ClearTier.of(stars)
            assertEquals(millis, tier.durationMillis)
            assertTrue("celebration ran long", tier.durationMillis <= 3000)
            assertEquals(tier.durationMillis / 1000f, model(stars).duration, 1e-4f)
        }
    }

    @Test
    fun aBetterClearAlwaysGetsMoreToLookAt() {
        val simple = model(1)
        val good = model(2)
        val perfect = model(3)
        assertTrue(simple.shells.size < good.shells.size)
        assertTrue(good.shells.size < perfect.shells.size)
        assertTrue(simple.ribbons.size < good.ribbons.size)
        assertTrue(good.ribbons.size < perfect.ribbons.size)
        assertTrue(simple.pieces.size < perfect.pieces.size)
    }

    @Test
    fun everyTierLaunchesWhatItPromises() {
        for (stars in 1..3) {
            val tier = ClearTier.of(stars)
            val built = model(stars)
            assertEquals(tier.anchorIndices.size, built.shells.size)
            assertEquals(tier.raysPerBurst, built.shells.first().rays.size)
            assertEquals(tier.grainsPerBurst, built.shells.first().grains.size)
            assertEquals(tier.ribbonPairs * 2, built.ribbons.size)
            assertEquals(tier.confettiCount, built.pieces.size)
        }
    }

    /* --------------------------------------------------------------- shells */

    @Test
    fun everyShellGoesOffInsideTheCelebration() {
        for (stars in 1..3) {
            val built = model(stars)
            for (shell in built.shells) {
                assertTrue("shell fired before the level landed", shell.launchAt >= 0f)
                assertTrue("shell burst at or before launch", shell.flight > 0f)
                assertTrue(
                    "burst outlived the celebration",
                    shell.burstAt + shell.life <= built.duration + 1e-3f
                )
                assertTrue("burst outlived the closing fade", shell.burstAt < built.duration)
            }
        }
    }

    @Test
    fun shellsAreFiredInTheOrderTheyAreLaidOut() {
        for (stars in 1..3) {
            val launches = model(stars).shells.map { it.launchAt }
            assertEquals(launches.sorted(), launches)
        }
    }

    @Test
    fun aShellClimbsAlongItsArcAndHandsOverToTheBurstPoint() {
        val shell = model(2).shells.first()
        val out = FloatArray(2)
        // One percent of the stage is close enough to read as "arrived" while
        // still catching a shell that stops short of its burst point.
        val near = max(width, height) * 0.01f

        assertTrue("nothing before launch", !shell.shellAt(shell.launchAt - 0.01f, out))
        assertTrue("nothing after the burst", !shell.shellAt(shell.burstAt, out))

        assertTrue(shell.shellAt(shell.launchAt, out))
        assertEquals(shell.startX, out[0], 0.5f)
        assertEquals(shell.startY, out[1], 0.5f)

        assertTrue(shell.shellAt(shell.burstAt - 0.001f, out))
        assertEquals(shell.burstX, out[0], near)
        assertEquals(shell.burstY, out[1], near)

        // Mid flight the shell is off its straight line, which is what stops the
        // launch reading as a sprite on a rail.
        val mid = shell.launchAt + shell.flight * 0.5f
        assertTrue(shell.shellAt(mid, out))
        val straightX = shell.startX + (shell.burstX - shell.startX) * 0.5f
        val straightY = shell.startY + (shell.burstY - shell.startY) * 0.5f
        assertTrue(
            "shell flew a straight line",
            hypot(out[0] - straightX, out[1] - straightY) > 1f
        )
    }

    @Test
    fun shellsAreFiredFromBelowTheStageAndBurstInsideIt() {
        for (stars in 1..3) {
            for (shell in model(stars).shells) {
                assertTrue("shell started on screen", shell.startY >= height * 0.9f)
                assertTrue("burst left the stage", shell.burstX in -width..2f * width)
                assertTrue("burst left the stage", shell.burstY in -height..2f * height)
            }
        }
    }

    @Test
    fun aRayOnlyEverGrowsOutwardAndNeverOvershoots() {
        val life = 0.9f
        var previous = -1f
        for (samples in 0..24) {
            val age = life * samples / 24f
            val length = burstRayLengthAt(age, life)
            assertTrue("ray shrank", length >= previous)
            assertTrue("ray overshot", length <= 1f)
            previous = length
        }
        assertEquals(0f, burstRayLengthAt(0f, life), 1e-5f)
        assertEquals(1f, burstRayLengthAt(life, life), 1e-5f)
    }

    @Test
    fun aRayFadesInQuicklyAndOutCompletely() {
        val life = 0.8f
        assertEquals(0f, burstRayAlphaAt(0f, life), 1e-5f)
        assertEquals(0f, burstRayAlphaAt(life, life), 1e-5f)
        assertEquals(0f, burstRayAlphaAt(life * 2f, life), 1e-5f)

        val peak = (0..20).map { burstRayAlphaAt(life * it / 20f, life) }.max()
        assertTrue("ray never lit up", peak > 0.9f)
        assertTrue(
            "ray was still bright at the end",
            burstRayAlphaAt(life * 0.92f, life) < 0.06f
        )
    }

    @Test
    fun aShellCoreFlashesAndIsGoneBeforeTheRaysAre() {
        val life = 0.9f
        assertTrue(burstFlashAlphaAt(0.01f, life) > 0f)
        assertTrue(burstFlashAlphaAt(0.02f, life) > burstFlashAlphaAt(0.30f, life))
        assertEquals(0f, burstFlashAlphaAt(life * 0.6f, life), 1e-5f)
        assertEquals(0f, burstFlashAlphaAt(-0.1f, life), 1e-5f)
    }

    @Test
    fun grainsSlowDownUnderDragAndDroopUnderGravity() {
        val out = FloatArray(2)
        val speed = 600f
        val angle = 0f

        burstGrainOffset(0f, 1f, speed, angle, 2.6f, 0f, out)
        assertEquals(0f, out[0], 1e-3f)
        assertEquals(0f, out[1], 1e-3f)

        burstGrainOffset(0.2f, 1f, speed, angle, 2.6f, 0f, out)
        val early = out[0]
        burstGrainOffset(0.6f, 1f, speed, angle, 2.6f, 0f, out)
        val late = out[0]

        assertTrue("grain went backwards", late > early)
        // Drag is asymptotic, so the second stretch of the flight is strictly
        // shorter than the first even though it is twice as long.
        assertTrue("grain did not decelerate", (late - early) < early)

        // Gravity pulls it down even while the thrust is still pushing sideways.
        burstGrainOffset(0.5f, 1f, speed, angle, 2.6f, 400f, out)
        assertTrue("grain ignored gravity", out[1] > 0f)
    }

    @Test
    fun grainsStopDeadAtTheEndOfTheirLife() {
        val out = FloatArray(2)
        burstGrainOffset(1f, 1f, 600f, 0.6f, 2.6f, 400f, out)
        val settledX = out[0]
        val settledY = out[1]
        burstGrainOffset(4f, 1f, 600f, 0.6f, 2.6f, 400f, out)
        assertEquals(settledX, out[0], 1e-3f)
        assertEquals(settledY, out[1], 1e-3f)
    }

    /* -------------------------------------------------------------- ribbons */

    @Test
    fun everyRibbonFinishesInsideTheCelebration() {
        for (stars in 1..3) {
            val built = model(stars)
            for (ribbon in built.ribbons) {
                assertTrue(ribbon.startAt >= 0f)
                assertTrue(
                    "ribbon outlived the celebration",
                    ribbon.startAt + ribbon.life <= built.duration + 1e-3f
                )
            }
        }
    }

    @Test
    fun ribbonsSweepTheWholeWayAcrossWithoutJumpingBack() {
        for (stars in 1..3) {
            for (ribbon in model(stars).ribbons) {
                val out = FloatArray(2)
                ribbon.pointAt(0f, out)
                val firstX = out[0]
                ribbon.pointAt(1f, out)
                val lastX = out[0]
                assertTrue(
                    "ribbon did not cross the screen",
                    abs(lastX - firstX) > width * 0.8f
                )

                var previousX = firstX
                var samples = 40
                while (samples-- > 0) {
                    ribbon.pointAt(1f - samples / 40f, out)
                    // A cubic can double back on itself horizontally, but only
                    // gently; a jump would mean the curve has been rebuilt wrong.
                    assertTrue(
                        "ribbon jumped",
                        abs(out[0] - previousX) < width * 0.25f
                    )
                    previousX = out[0]
                }
            }
        }
    }

    @Test
    fun aRibbonHeadSweepsForwardMonotonically() {
        val life = 0.8f
        assertEquals(0f, ribbonHeadAt(0f, life), 1e-5f)
        assertEquals(0f, ribbonHeadAt(-0.1f, life), 1e-5f)
        assertEquals(1f, ribbonHeadAt(life, life), 1e-5f)
        assertEquals(1f, ribbonHeadAt(life * 2f, life), 1e-5f)

        var previous = -1f
        for (samples in 0..40) {
            val head = ribbonHeadAt(life * samples / 40f, life)
            assertTrue("head stalled", head >= previous)
            previous = head
        }
    }

    @Test
    fun aRibbonIsBrightInTheMiddleAndGoneAtBothEnds() {
        val life = 0.9f
        assertEquals(0f, ribbonAlphaAt(0f, life), 1e-5f)
        assertEquals(0f, ribbonAlphaAt(life, life), 1e-5f)
        assertTrue(ribbonAlphaAt(life * 0.35f, life) > 0.9f)
        assertTrue(ribbonAlphaAt(life * 0.9f, life) < 0.2f)
    }

    @Test
    fun aNarrowRibbonIsNarrowerThanAWideOne() {
        val simple = model(1).ribbons
        val perfect = model(3).ribbons
        assertTrue(perfect.maxOf { it.width } > simple.maxOf { it.width })
    }

    /* ------------------------------------------------------ falling + fades */

    @Test
    fun fallingPiecesAppearLateAndBuryThemselvesBeforeTheyEnd() {
        val built = model(2)
        for (piece in built.pieces) {
            assertEquals(0f, piece.alphaAt(piece.delay - 0.01f), 1e-5f)
            assertEquals(0f, piece.alphaAt(piece.delay + piece.life + 0.01f), 1e-5f)
            assertTrue(piece.alphaAt(piece.delay + piece.life * 0.25f) > 0.85f)
            assertTrue(piece.alphaAt(piece.delay + piece.life * 0.9f) < 0.35f)
        }
    }

    @Test
    fun piecesKeepFallingAllTheWayThroughTheirOwnLife() {
        val built = model(2)
        val out = FloatArray(2)
        for (piece in built.pieces.take(12)) {
            piece.positionAt(piece.delay, out)
            val startY = out[1]
            piece.positionAt(piece.delay + piece.life, out)
            assertTrue("piece did not fall", out[1] > startY)
            assertTrue("piece teleported", abs(out[1] - startY) < height * 2f)
        }
    }

    @Test
    fun piecesWobbleSidewaysInsteadOfFallingLikeStones() {
        // Driven straight, with no drift and no gravity, so the only thing that
        // can move the piece sideways is its own wobble.
        val piece = CelebrationPiece(
            x = 0f,
            y = 0f,
            vx = 0f,
            vy = 0f,
            drag = 0f,
            gravity = 0f,
            delay = 0f,
            life = 1f,
            size = 8f,
            kind = 1,
            spin = 0f,
            phase = 0f,
            wobbleAmp = 20f,
            wobbleFreq = 6f,
            color = NeonCyan
        )
        val out = FloatArray(2)
        val samples = (0..24).map {
            piece.positionAt(it / 24f, out)
            out[0]
        }
        assertTrue("piece fell dead straight", samples.max() - samples.min() > 20f)
    }

    @Test
    fun theRainStartsWhileTheFireworksAreStillGoingAndEndsInsideTheBudget() {
        for (stars in 1..3) {
            val built = model(stars)
            val first = built.pieces.minOf { it.delay }
            assertTrue("rain started too late", first < built.duration * 0.5f)
            for (piece in built.pieces) {
                assertTrue(
                    "rain outlived the celebration",
                    piece.delay + piece.life <= built.duration + 1e-3f
                )
            }
        }
    }

    @Test
    fun theGlitterKeepsFallingIntoTheClosingFade() {
        for (stars in 1..3) {
            val built = model(stars)
            val last = built.glitter.maxBy { it.delay }
            assertTrue("no glitter", built.glitter.isNotEmpty())
            assertTrue("glitter ended early", last.delay + last.life <= built.duration + 1e-3f)
            assertTrue("glitter never appeared", last.delay > 0f)
        }
    }

    @Test
    fun theScreenFlashIsOverAlmostImmediately() {
        assertTrue(screenFlashAlphaAt(0f) < 0.2f)
        assertTrue("flash never fired", screenFlashAlphaAt(0.06f) > 0.9f)
        assertTrue("flash outlasted itself", screenFlashAlphaAt(FlashSeconds) < 0.02f)
        assertEquals(0f, screenFlashAlphaAt(FlashSeconds * 2f), 1e-5f)
    }

    @Test
    fun theCelebrationAlwaysDissesolvesBeforeItEnds() {
        val duration = 3f
        assertEquals(1f, celebrationFadeAt(0f, duration), 1e-5f)
        assertEquals(1f, celebrationFadeAt(duration - ExitFadeSeconds, duration), 1e-5f)
        assertTrue(celebrationFadeAt(duration, duration) < 0.001f)
        assertEquals(0f, celebrationFadeAt(duration + 1f, duration), 1e-5f)

        var previous = 1f
        for (samples in 0..60) {
            val alpha = celebrationFadeAt(duration * samples / 60f, duration)
            assertTrue("fade rose again", alpha <= previous)
            previous = alpha
        }
    }

    @Test
    fun dragTravelIsBoundedAndStartsAtFullSpeed() {
        assertEquals(0f, dragTravel(0f, 2.6f), 1e-6f)
        assertEquals(1f / 2.6f, dragTravel(1000f, 2.6f), 1e-4f)
        assertEquals(0.5f, dragTravel(0.5f, 0f), 1e-6f)
    }

    @Test
    fun theBannerIsAlwaysReadableWhenItNeedsToBe() {
        for (tier in ClearTier.values()) {
            val show = bannerAlpha(
                t = tier.clearAt + 0.2f,
                start = tier.clearAt,
                fadeIn = 0.06f,
                fadeOutStart = 2f,
                fadeOutEnd = 2.1f
            )
            assertEquals(1f, show, 1e-5f)
            assertEquals(
                0f,
                bannerAlpha(
                    t = tier.clearAt - 0.01f,
                    start = tier.clearAt,
                    fadeIn = 0.06f,
                    fadeOutStart = 2f,
                    fadeOutEnd = 2.1f
                ),
                1e-5f
            )
        }
    }
}
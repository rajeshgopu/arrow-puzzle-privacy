package com.gopu.arrow.puzzle.game.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A screen swap is two screens composed at once rather than two screens in
 * sequence, and every guarantee that stops the window ever going blank lives in
 * the curves in [ScreenTransition.kt] and nowhere else. These pin them without a
 * device: the swap is short, the two alphas can never sum to less than one (which
 * is what would let the window show through the middle of the swap), the
 * destination is solid before the clock ends, and neither layer ever inverts or
 * moves far enough to read as a jump cut.
 */
class ScreenTransitionTest {

    /** Every point on the swap clock, at a typical frame interval over the window. */
    private val frames = 40

    @Test
    fun swapIsInsideTheRequestedWindow() {
        assertTrue(
            "swap is ${ScreenSwapMillis}ms, outside the 150-250ms window",
            ScreenSwapMillis in 150..250
        )
    }

    @Test
    fun theScreenOnShowIsNeverFadedByAClockThatHasNotStarted() {
        // The clock sits at zero until the first navigation, and the incoming
        // curve is zero at zero - which is what a destination has to start at,
        // and exactly wrong for the menu the splash hands off to. A screen that
        // is not arriving has to ignore the clock entirely.
        for (step in 0..frames) {
            val progress = step / frames.toFloat()
            assertEquals(
                "screen on show at $progress is faded",
                1f,
                screenAlphaAt(progress, swapping = false),
                0f
            )
            assertEquals(
                "screen on show at $progress is scaled",
                1f,
                screenScaleAt(progress, swapping = false),
                0f
            )
        }
    }

    @Test
    fun anArrivingScreenStillStartsFromNothing() {
        // The other half of the same decision: while a swap is under way the
        // destination has to start invisible, or it pops in on top of the screen
        // being left instead of fading up through it.
        assertEquals(0f, screenAlphaAt(0f, swapping = true), 0f)
        assertTrue(
            "an arriving screen is already its own size",
            screenScaleAt(0f, swapping = true) < 1f
        )
        assertEquals(1f, screenAlphaAt(1f, swapping = true), 0f)
        assertEquals(1f, screenScaleAt(1f, swapping = true), 0.001f)
    }

    @Test
    fun theOutgoingScreenHoldsTheWindowUntilTheDestinationIsSolid() {
        // The outgoing screen is what the player is looking at while the
        // destination is composed, so it has to be fully opaque for the whole of
        // that, which is the state the clock starts in and the state it leaves.
        assertEquals(1f, screenOutAlphaAt(0f), 0f)
        assertEquals(0f, screenInAlphaAt(0f), 0f)
    }

    @Test
    fun theWindowNeverShowsThroughBetweenTheTwoScreens() {
        // The whole point of keeping both screens composed. If the two alphas
        // could sum to less than one, the bare window would show through the
        // middle of every swap - the blank frame this exists to prevent.
        for (step in 0..frames) {
            val progress = step / frames.toFloat()
            val coverage = screenInAlphaAt(progress) + screenOutAlphaAt(progress)
            assertTrue(
                "at $progress the two screens only cover $coverage of the window",
                coverage >= 0.999f
            )
        }
    }

    @Test
    fun theDestinationIsFullyOpaqueBeforeTheClockEnds() {
        // The outgoing screen is dropped when the clock reaches one, so at that
        // point the destination has to already be solid or the drop is visible.
        val lastFrame = (frames - 1) / frames.toFloat()
        assertEquals(1f, screenInAlphaAt(lastFrame), 0.001f)
    }

    @Test
    fun nothingInTheSwapInvertsOrJumps() {
        // Both curves start from the state the swap begins in: nothing on screen,
        // then the outgoing one alone.
        var previousIn = 0f
        var previousOut = 1f
        for (step in 0..frames) {
            val progress = step / frames.toFloat()
            val inside = screenInAlphaAt(progress)
            val outside = screenOutAlphaAt(progress)

            assertTrue("incoming alpha $inside at $progress", inside in 0f..1f)
            assertTrue("outgoing alpha $outside at $progress", outside in 0f..1f)
            assertTrue("incoming alpha went backwards at $progress", inside >= previousIn - 0.001f)
            assertTrue("outgoing alpha went backwards at $progress", outside <= previousOut + 0.001f)

            previousIn = inside
            previousOut = outside
        }
    }

    @Test
    fun theScaleIsASettleRatherThanAJumpCut() {
        // Enough to read as motion, not enough to look like a different screen.
        // The destination arrives slightly small and settles to exactly its own
        // size; the outgoing one starts at its own size and is pushed slightly
        // back, so neither layer is ever the wrong shape at either end.
        assertEquals(1f, screenInScaleAt(1f), 0.001f)
        assertEquals(1f, screenOutScaleAt(0f), 0.001f)

        val inward = 1f - screenInScaleAt(0f)
        assertTrue("the destination moves $inward of its size on the way in", inward in 0.01f..0.06f)

        val outward = screenOutScaleAt(1f) - 1f
        assertTrue("the outgoing screen moves $outward of its size on the way out", outward in 0.005f..0.06f)
    }

    @Test
    fun theScaleMovesMonotonically() {
        var previous = 0f
        for (step in 0..frames) {
            val scale = screenInScaleAt(step / frames.toFloat())
            assertTrue("incoming scale stalled or reversed at $step", scale >= previous - 0.001f)
            previous = scale
        }
    }

    @Test
    fun theBackstopIsLongEnoughToBeUnnoticedAndShortEnoughToNotStrand() {
        // It should never be reached: a full-size destination reports its size
        // within a frame. It exists so a destination that somehow never lays out
        // cannot leave the player looking at a half faded screen for ever.
        assertTrue(
            "backstop is ${ScreenReadyTimeoutMillis}ms",
            ScreenReadyTimeoutMillis in 200..1000
        )
    }
}
package com.gopu.arrow.puzzle.game.app

/**
 * Screen swaps, as curves rather than as a routed screen.
 *
 * Tapping a target swaps what is on top of the window in one frame, but the
 * screen underneath that target has not been composed, measured or drawn yet,
 * and on a slow device that takes longer than a frame. Cutting straight to it
 * therefore shows the bare window background for as long as the destination
 * takes to appear. Nothing about the destination can prevent that - the work has
 * to happen on the main thread - so what stops the blank is that it happens
 * *underneath* the screen the player is leaving, which stays on top and fully
 * opaque for the whole of that time.
 *
 * So a swap is two screens composed at once, not two screens in sequence:
 *
 *  - the destination is composed, measured, laid out and drawn on the very frame
 *    after the tap, behind the outgoing screen and at zero alpha. Nothing waits
 *    to be "loaded"; the cost is simply paid where it cannot be seen, which is
 *    also why the destination's own entrance animations have already started by
 *    the time the player sees any of it.
 *  - the outgoing screen holds at full opacity until the destination has been
 *    measured *and* one real frame has been produced, which is what the caller
 *    waits on before starting the clock. Until then the player is looking at
 *    exactly the frame they tapped, so there is no blank, no white and no
 *    black: just the last good frame, still there.
 *  - one clock then cross-fades the two. The curves below never let the pair
 *    sum to less than one, so the two alphas cannot dip and darken the window in
 *    the middle of the swap, and the destination is fully opaque before the clock
 *    ends - so the frame the outgoing screen is dropped on is already the
 *    finished destination.
 *
 * The whole transition is two `graphicsLayer` reads and one animatable. Nothing
 * is relaid out, nothing recomposes, and no layout pass runs twice, which is
 * what keeps it affordable on a low-end device: the cost of the swap is the
 * destination's own first frame plus 200ms of two layer transforms.
 */

/** How long the cross-fade runs once the destination is on screen. */
internal const val ScreenSwapMillis = 200

/**
 * How long the swap waits for the destination to be measured and drawn before
 * giving up and swapping anyway. A full-size destination always reports its size
 * within a frame, so this only exists so that a destination which somehow never
 * lays out can never strand the player on a screen that is half faded out.
 */
internal const val ScreenReadyTimeoutMillis = 600L

/** Opacity of the screen being arrived at, once a swap is under way. */
internal fun screenInAlphaAt(progress: Float): Float = ramp(0f, ScreenInAlphaEnd, progress)

/**
 * Opacity of the screen on show.
 *
 * The incoming curve is zero at rest, which is exactly right *during* a swap - the
 * destination has to start invisible - and exactly wrong outside one, where the
 * screen on show is the menu and not the destination of anything. So the swap is
 * the thing being asked about: with no swap in flight the screen on show is at
 * full strength and its own size, whatever the clock happens to be sitting at.
 */
internal fun screenAlphaAt(progress: Float, swapping: Boolean): Float =
    if (swapping) screenInAlphaAt(progress) else 1f

/** Scale of the screen on show: the settle while swapping, its own size otherwise. */
internal fun screenScaleAt(progress: Float, swapping: Boolean): Float =
    if (swapping) screenInScaleAt(progress) else 1f

/**
 * Opacity of the screen being left. It fades across the whole window rather than
 * cutting out early, so the two alphas overlap the entire time and the window
 * never shows through between them.
 */
internal fun screenOutAlphaAt(progress: Float): Float = 1f - ramp(0f, 1f, progress)

/** Scale of the arriving screen: a short settle up into place. */
internal fun screenInScaleAt(progress: Float): Float =
    lerp(ScreenInScale, 1f, ramp(0f, 1f, progress))

/** Scale of the leaving screen: pushed very slightly back as it fades. */
internal fun screenOutScaleAt(progress: Float): Float =
    lerp(1f, ScreenOutScale, ramp(0f, 1f, progress))

/**
 * A clamped, eased 0 to 1 ramp between two edges, smooth at both ends so no
 * curve in the swap ever starts or stops with a visible step.
 */
private fun ramp(edge0: Float, edge1: Float, value: Float): Float {
    if (edge1 <= edge0) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Linear interpolation, kept local so the curves stay dependency free. */
private fun lerp(from: Float, to: Float, fraction: Float): Float =
    from + (to - from) * fraction

/** Portion of the swap the arriving screen takes to reach full opacity. */
private const val ScreenInAlphaEnd = 0.70f

/** Scale the arriving screen starts at, and how far back the leaving one goes. */
private const val ScreenInScale = 0.965f
private const val ScreenOutScale = 1.015f

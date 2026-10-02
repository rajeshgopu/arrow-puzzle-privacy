package com.gopu.arrow.puzzle.game.ui

/**
 * The launch choreography as pure curves, so the splash-to-menu handoff can be
 * pinned by tests without a device.
 *
 * The launch is one animation read by two layers rather than two screens being
 * switched. Two clocks drive it:
 *
 *  - the intro clock runs while the brand is on screen and settles the wordmark
 *    and arrow into place;
 *  - the reveal clock runs once, at the handoff. It is 0 while the splash owns
 *    the screen and 1 once the menu owns it, and both layers read that same
 *    clock, so they can never disagree about a frame.
 *
 * Because the menu is composed underneath the overlay from the very first frame,
 * the reveal only has to dissolve a screen that has already been measured, laid
 * out and drawn. Both clocks are read inside `graphicsLayer` blocks only, so a
 * frame of the handoff repaints two layers and recomposes nothing.
 *
 * Every curve is clamped, so a frame that lands early or late can never
 * overshoot past fully transparent or fully opaque, and at [SplashRevealMillis]
 * the splash is exactly invisible while the menu is exactly visible. That is what
 * lets the overlay be unmounted at the end of the handoff without a pop.
 */

/** How long the wordmark and arrow take to settle into place at launch. */
internal const val SplashIntroMillis = 460

/** How long the brand holds at full strength before the handoff starts. */
internal const val SplashHoldMillis = 1200L

/**
 * How long the splash-to-menu handoff takes. Short enough to read as one
 * gesture, long enough that neither layer has to snap: every curve below is
 * finished or safely on its way by the end of it.
 */
internal const val SplashRevealMillis = 320

/**
 * Opacity of the splash field: the ivory ground the brand sits on. It is fully
 * painted from the first frame and reaches zero exactly when the reveal ends, so
 * the cross-fade into the menu backdrop has no colour to move.
 */
internal fun splashFieldAlphaAt(reveal: Float): Float = 1f - ramp(0f, 1f, reveal)

/**
 * Opacity of the wordmark and arrow block.
 *
 * The two clocks are multiplied rather than switched, so the block fades in on
 * the intro and is gone by the middle of the handoff: the menu is already
 * arriving underneath by then and the two never ghost over each other.
 */
internal fun splashMarkAlphaAt(intro: Float, reveal: Float): Float =
    ramp(0f, 1f, intro) * (1f - ramp(0f, 0.5f, reveal))

/**
 * Scale of the wordmark and arrow block: oversized and settling during the
 * intro, then drifting further outward on the way out so the mark leaves along
 * the same motion the menu arrives on.
 */
internal fun splashMarkScaleAt(intro: Float, reveal: Float): Float =
    lerp(SplashMarkIntroScale, 1f, ramp(0f, 1f, intro)) *
        lerp(1f, SplashMarkOutScale, ramp(0f, 1f, reveal))

/**
 * Opacity of the menu underneath. It starts once the mark is on its way out and
 * is fully opaque before the splash field has finished leaving, so the last
 * frame of the handoff is the finished menu and nothing arrives after it.
 */
internal fun homeAlphaAt(reveal: Float): Float = ramp(HomeFadeStart, HomeFadeEnd, reveal)

/**
 * Scale of the menu underneath. It arrives slightly oversized and settles to one,
 * in the same direction the splash mark leaves in, so the handoff reads as one
 * continuous drift. Oversized rather than undersized on purpose: it can only
 * ever overrun the window and be clipped by it, never inset and expose a band of
 * bare background at the edges.
 */
internal fun homeScaleAt(reveal: Float): Float =
    lerp(HomeArrivalScale, 1f, ramp(HomeFadeStart, HomeFadeEnd, reveal))

/** Scale the splash mark starts at and settles out of during the intro. */
private const val SplashMarkIntroScale = 1.06f

/** Scale the splash mark has drifted to by the end of the handoff. */
private const val SplashMarkOutScale = 1.10f

/** Scale the menu arrives at and settles out of. */
private const val HomeArrivalScale = 1.025f

/** Portion of the handoff the menu starts fading in over. */
private const val HomeFadeStart = 0.10f

/** Portion of the handoff by which the menu is fully opaque. */
private const val HomeFadeEnd = 0.86f

/**
 * A clamped, eased 0 to 1 ramp between two edges, smooth at both ends so no
 * curve in the launch ever starts or stops with a visible step.
 */
private fun ramp(edge0: Float, edge1: Float, value: Float): Float {
    if (edge1 <= edge0) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Linear interpolation, kept local so the curves stay dependency free. */
private fun lerp(from: Float, to: Float, fraction: Float): Float =
    from + (to - from) * fraction
package com.gopu.arrow.puzzle.game.ui

/**
 * The launch choreography as pure curves, so the splash-to-menu handoff and the
 * arrival of every piece of the launch scene can be pinned by tests without a
 * device.
 *
 * The launch is one animation read by two layers rather than two screens being
 * switched. Two clocks drive it:
 *
 *  - the intro clock runs while the brand is on screen and hands each piece of
 *    the scene its own slice of the entrance, so the arrow lands first and the
 *    loading bar is the last thing to arrive;
 *  - the reveal clock runs once, at the handoff. It is 0 while the splash owns
 *    the screen and 1 once the menu owns it, and both layers read that same
 *    clock, so they can never disagree about a frame.
 *
 * Because the menu is composed underneath the overlay from the very first frame,
 * the reveal only has to dissolve a screen that has already been measured, laid
 * out and drawn. Both clocks are read inside `graphicsLayer` blocks only, so a
 * frame of the handoff repaints its layers and recomposes nothing.
 *
 * Every curve is clamped, so a frame that lands early or late can never overshoot
 * past fully transparent or fully opaque, and at [SplashRevealMillis] the splash
 * is exactly invisible while the menu is exactly visible. That is what lets the
 * overlay be unmounted at the end of the handoff without a pop.
 */

/** How long the launch scene takes to assemble itself at launch. */
internal const val SplashIntroMillis = 620

/** How long the brand holds at full strength before the handoff starts. */
internal const val SplashHoldMillis = 1350L

/**
 * How long the splash-to-menu handoff takes. Short enough to read as one
 * gesture, long enough that neither layer has to snap: every curve below is
 * finished or safely on its way by the end of it.
 */
internal const val SplashRevealMillis = 340

/**
 * How much of the intro one piece of the scene spends arriving. Every start below
 * plus this span has to land at or before 1, or the last piece would still be
 * moving when the handoff began.
 */
internal const val SplashPieceSpan = 0.30f

/** The hero arrow: first thing on the screen, because everything else points at it. */
internal const val SplashArrowStart = 0f

/**
 * When each of the four floating cubes leaves the edge, in intro fractions. One
 * cube per 0.12 keeps the scatter reading as a scatter rather than as a queue.
 */
internal val SplashCubeStarts = listOf(0.12f, 0.24f, 0.36f, 0.46f)

/** The stones, which need the cubes placed before they can weave between them. */
internal const val SplashGemStart = 0.54f

/** The plaque: the brand itself, and the last thing to move before the loader. */
internal const val SplashPlaqueStart = 0.60f

/** The loading bar, which cannot appear until there is a brand to be loading. */
internal const val SplashLoaderStart = 0.70f

/**
 * Opacity of the splash field: the sky the whole scene stands on. It is fully
 * painted from the first frame and reaches zero exactly when the reveal ends, so
 * the cross-fade into the menu backdrop has no colour to move.
 */
internal fun splashFieldAlphaAt(reveal: Float): Float = 1f - ramp(0f, 1f, reveal)

/**
 * Opacity of the launch scene as a whole - the arrow, the cubes, the stones, the
 * plaque and the bar.
 *
 * The two clocks are multiplied rather than switched, so the scene assembles on
 * the intro and is gone by the middle of the handoff: the menu is already
 * arriving underneath by then and the two never ghost over each other.
 */
internal fun splashMarkAlphaAt(intro: Float, reveal: Float): Float =
    ramp(0f, 1f, intro) * (1f - ramp(0f, 0.5f, reveal))

/**
 * Scale of the launch scene: oversized and settling during the intro, then
 * drifting further outward on the way out so the scene leaves along the same
 * motion the menu arrives on.
 */
internal fun splashMarkScaleAt(intro: Float, reveal: Float): Float =
    lerp(SplashMarkIntroScale, 1f, ramp(0f, 1f, intro)) *
        lerp(1f, SplashMarkOutScale, ramp(0f, 1f, reveal))

/**
 * Opacity of one piece of the scene: nothing at all before its own start, and
 * full the moment it lands. Every piece is invisible before its start rather than
 * faint, so nothing can ever be seen sitting in the sky waiting for its cue.
 */
internal fun splashPieceAlphaAt(intro: Float, start: Float): Float =
    ramp(start, start + SplashPieceSpan, intro)

/**
 * Scale of one piece: it arrives from [from], sails a little past its resting
 * size and settles back onto one.
 *
 * The travel is a back-out ease rather than a ramp, so a piece cannot arrive
 * looking decelerated into place. [pop] is both the strength of the overshoot and
 * the ceiling it is allowed to reach: the ease is scaled so that however the
 * piece is easing, its scale never climbs above `1 + pop`, which is what lets the
 * layout below treat the overshoot as a fixed budget rather than a guess.
 */
internal fun splashPieceScaleAt(intro: Float, start: Float, from: Float, pop: Float): Float {
    val t = splashPieceAlphaAt(intro, start)
    val overshoot = SplashBackEase * pop
    val shifted = t - 1f
    val eased = 1f + (overshoot + 1f) * shifted * shifted * shifted + overshoot * shifted * shifted
    return lerp(from, 1f, eased)
}

/**
 * How much of its entry travel one piece has left: the whole of it on the frame
 * it starts, none of it once it has landed. Feeding this to a translation is how
 * a piece flies in from off its own edge instead of fading in where it belongs.
 */
internal fun splashPieceTravelAt(intro: Float, start: Float): Float =
    1f - splashPieceAlphaAt(intro, start)

/**
 * How far a piece is still turned from where it comes to rest, in degrees.
 * Signed, so the caller passes the direction the piece is travelling from.
 */
internal fun splashPieceTurnAt(intro: Float, start: Float, from: Float): Float =
    from * splashPieceTravelAt(intro, start)

/**
 * Opacity of the menu underneath. It starts once the scene is on its way out and
 * is fully opaque before the splash field has finished leaving, so the last
 * frame of the handoff is the finished menu and nothing arrives after it.
 */
internal fun homeAlphaAt(reveal: Float): Float = ramp(HomeFadeStart, HomeFadeEnd, reveal)

/**
 * Scale of the menu underneath. It arrives slightly oversized and settles to one,
 * in the same direction the splash scene leaves in, so the handoff reads as one
 * continuous drift. Oversized rather than undersized on purpose: it can only
 * ever overrun the window and be clipped by it, never inset and expose a band of
 * bare background at the edges.
 */
internal fun homeScaleAt(reveal: Float): Float =
    lerp(HomeArrivalScale, 1f, ramp(HomeFadeStart, HomeFadeEnd, reveal))

/** Scale the launch scene starts at and settles out of during the intro. */
private const val SplashMarkIntroScale = 1.06f

/** Scale the launch scene has drifted to by the end of the handoff. */
private const val SplashMarkOutScale = 1.10f

/** Scale the menu arrives at and settles out of. */
private const val HomeArrivalScale = 1.025f

/** Portion of the handoff the menu starts fading in over. */
private const val HomeFadeStart = 0.10f

/** Portion of the handoff by which the menu is fully opaque. */
private const val HomeFadeEnd = 0.86f

/**
 * How hard a piece's arrival ease pushes past one, per unit of [pop]. The stock
 * back-out constant is 1.70158; this is a harder push, because the ramps the ease
 * runs between are already eased and a gentle one would never visibly arrive.
 */
private const val SplashBackEase = 14f

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

package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan

/**
 * Every dimension, intensity and timing knob of the tap glow pulse.
 *
 * Nothing here is a pixel value. Each one is a multiplier on the tapped arrow's
 * own rendered size, so the effect is the same size relative to its arrow on any
 * screen, density, board size, level grid or arrow shape, and it needs no work
 * when the board is measured or re-measured.
 *
 * A style picks one of these; the effect itself holds no configuration, which is
 * what lets a single pooled [TapGlowPulseEffect] play every style in turn.
 */
internal data class TapGlowPulseConfig(
    /** How long the whole pulse lasts, in milliseconds. Kept short on purpose. */
    val durationMillis: Int = DefaultDurationMillis,

    /** Curve the expansion and the fade both run on. */
    val expansionEasing: Easing = FastOutSlowInEasing,

    /** Widest radius of the soft pulse disc, as a fraction of the arrow's size. */
    val pulseRadiusMultiplier: Float = DefaultPulseRadiusMultiplier,

    /** Widest radius of the ambient glow around the pulse, as a fraction of the arrow's size. */
    val glowRadiusMultiplier: Float = DefaultGlowRadiusMultiplier,

    /** Radius of the thin light ring, as a fraction of the arrow's size. */
    val ringRadiusMultiplier: Float = DefaultRingRadiusMultiplier,

    /** Thickness the ring starts at, as a fraction of the arrow's size. */
    val ringThicknessMultiplier: Float = DefaultRingThicknessMultiplier,

    /** Radius of the hot centre, as a fraction of the arrow's size. */
    val coreRadiusMultiplier: Float = DefaultCoreRadiusMultiplier,

    /** Alpha the pulse reaches on its first frame. */
    val glowIntensity: Float = DefaultGlowIntensity,

    /** Alpha of the light ring. Deliberately low: it is a hint, not a shockwave. */
    val ringIntensity: Float = DefaultRingIntensity,

    /** Alpha of the hot centre. */
    val coreIntensity: Float = DefaultCoreIntensity,

    /** How much the arrow itself brightens while its pulse is running. */
    val arrowGlowBoost: Float = DefaultArrowGlowBoost,

    /**
     * How much of its alpha the pulse has lost by the end.
     *
     * `1` fades out completely, `0` holds full brightness for the whole pulse.
     */
    val fadeAmount: Float = 1f
)

/**
 * The soft glow pulse played on the arrow the player just tapped.
 *
 * One reusable instance holds one pulse's state — where it is centred, how big
 * the arrow under it is and what colour that arrow is — and is re-armed in place
 * for the next tap. It never touches the arrow: nothing here moves, turns,
 * bends or resizes the geometry, it only adds light around and over it. Every
 * radius is the arrow's own size times a multiplier, and every colour is derived
 * from the arrow's own colour, so a straight or bent arrow in any of the palette
 * colours gets the same treatment.
 *
 * Which shape the pulse takes is entirely [config]'s business, supplied per draw
 * rather than held here, so the same instance plays the quietest style and the
 * most elaborate one without being rebuilt.
 */
internal class TapGlowPulseEffect {

    private var center = Offset.Zero
    private var arrowSize = 0f
    private var color = NeonCyan

    /**
     * Points this effect at an arrow, ready for the next [draw].
     *
     * [arrowSize] is that arrow's own rendered size and [color] its own colour,
     * so the pulse is measured and tinted against whatever it is marking rather
     * than against anything assumed about the board.
     */
    fun arm(center: Offset, arrowSize: Float, color: Color) {
        this.center = center
        this.arrowSize = arrowSize
        this.color = color
    }

    /**
     * Draws the pulse at [progress], 0 at the instant of the tap and 1 when it is
     * over, shaped by [config]. Does nothing once it is finished, so an idle
     * effect costs one compare.
     */
    fun draw(scope: DrawScope, progress: Float, config: TapGlowPulseConfig) {
        if (arrowSize <= 0f) return

        val t = config.expansionEasing.transform(progress.coerceIn(0f, 1f))
        val alpha = pulseAlphaAt(t, config.glowIntensity, config.fadeAmount)
        if (alpha <= AlphaFloor) return

        scope.drawTapGlowPulse(center, arrowSize, color, t, alpha, config)
    }
}

/**
 * Draws the pulse itself: a soft glow disc, an expanding pulse front, a thin
 * light ring and a hot centre, all additive and all centred on the arrow.
 *
 * The soft falloff is built from a handful of concentric discs rather than a
 * radial brush, because a brush is a fresh object every frame and these are
 * values that change every frame. The discs are cheap, allocate nothing and read
 * as a smooth glow, and they composite additively so overlaps brighten towards
 * the centre instead of muddying.
 */
private fun DrawScope.drawTapGlowPulse(
    center: Offset,
    arrowSize: Float,
    color: Color,
    progress: Float,
    alpha: Float,
    config: TapGlowPulseConfig
) {
    val warm = lerp(color, NeonCore, 0.28f)
    val hot = lerp(color, NeonCore, 0.62f)

    // Ambient glow: the widest, faintest layer, so the pulse lights the plate
    // around the arrow as well as the arrow itself.
    val glowRadius = glowRadiusAt(arrowSize, progress, config.glowRadiusMultiplier)
    for (layer in GlowLayers - 1 downTo 0) {
        val fraction = (layer + 1).toFloat() / GlowLayers
        drawCircle(
            color = color.copy(alpha = alpha * layerWeight(fraction) * GlowLayerGain * HaloGain),
            radius = glowRadius * fraction,
            center = center,
            blendMode = BlendMode.Plus
        )
    }

    // The pulse front: tighter, hotter, and the part that actually reads as an
    // expanding wave leaving the arrow.
    val pulseRadius = pulseRadiusAt(arrowSize, progress, config.pulseRadiusMultiplier)
    for (layer in GlowLayers - 1 downTo 0) {
        val fraction = (layer + 1).toFloat() / GlowLayers
        drawCircle(
            color = warm.copy(alpha = alpha * layerWeight(fraction) * GlowLayerGain),
            radius = pulseRadius * fraction,
            center = center,
            blendMode = BlendMode.Plus
        )
    }

    // The subtle radial light ring: a bright rim with a wider, fainter skirt, so
    // the wave has an edge without reading as a hard shockwave.
    val ringAlpha = pulseAlphaAt(progress, config.ringIntensity, config.fadeAmount)
    if (ringAlpha > AlphaFloor) {
        val ringRadius = ringRadiusAt(arrowSize, progress, config.ringRadiusMultiplier)
        val ringWidth = ringThicknessAt(arrowSize, progress, config.ringThicknessMultiplier)
        drawCircle(
            color = color.copy(alpha = ringAlpha * RingSkirtGain),
            radius = ringRadius,
            center = center,
            style = Stroke(width = ringWidth * RingSkirtWidth),
            blendMode = BlendMode.Plus
        )
        drawCircle(
            color = hot.copy(alpha = ringAlpha),
            radius = ringRadius,
            center = center,
            style = Stroke(width = ringWidth),
            blendMode = BlendMode.Plus
        )
    }

    // Hot centre: brightest on the tap frame and gone within the first third of
    // the pulse, which is what sells the first instant as "immediate".
    val coreRadius = coreRadiusAt(arrowSize, progress, config.coreRadiusMultiplier)
    if (coreRadius > 0f) {
        drawCircle(
            color = NeonCore.copy(alpha = pulseAlphaAt(progress, config.coreIntensity, config.fadeAmount)),
            radius = coreRadius,
            center = center,
            blendMode = BlendMode.Plus
        )
    }
}

/** How far a disc of radius `radius * [fraction]` contributes at its own edge. */
private fun layerWeight(fraction: Float): Float {
    val falloff = 1f - fraction
    return falloff * falloff + LayerFloor
}

/**
 * How wide the expanding pulse disc is, as a fraction of the arrow's own size.
 *
 * It opens at [StartRadiusFraction] of its widest point rather than at nothing —
 * a pulse that starts from zero reads as a late pop — and reaches exactly
 * `arrowSize * multiplier` at the end of its eased run.
 */
internal fun pulseRadiusAt(arrowSize: Float, progress: Float, multiplier: Float): Float =
    arrowSize * multiplier * growthAt(progress)

/**
 * How wide the ambient glow around the pulse is, as a fraction of the arrow's
 * own size. Wider than [pulseRadiusAt] and much fainter, so the pulse has room
 * to breathe without ever competing with a neighbouring arrow.
 */
internal fun glowRadiusAt(arrowSize: Float, progress: Float, multiplier: Float): Float =
    arrowSize * multiplier * growthAt(progress)

/**
 * How far the ring has travelled, as a fraction of the arrow's own size.
 *
 * The ring leads the soft pulse and lags the glow, so the three layers separate
 * as they grow instead of sitting on top of each other.
 */
internal fun ringRadiusAt(arrowSize: Float, progress: Float, multiplier: Float): Float =
    arrowSize * multiplier * growthAt(progress)

/**
 * How thick the ring is, in the arrow's own units.
 *
 * It thins as it travels, the way a real wave does, and never thins past a floor
 * so it can still be drawn on a very small arrow.
 */
internal fun ringThicknessAt(arrowSize: Float, progress: Float, multiplier: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val width = arrowSize * multiplier * (1f - RingThinBy * t)
    return maxOf(width, arrowSize * MinRingThickness)
}

/**
 * How wide the hot centre is, in the arrow's own units.
 *
 * Squared decay: full size on the tap frame, unnoticeable by a third of the way
 * through, which is what makes the pulse feel instant rather than ramped.
 */
internal fun coreRadiusAt(arrowSize: Float, progress: Float, multiplier: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val remaining = 1f - t
    return arrowSize * multiplier * remaining * remaining
}

/**
 * How visible a layer is.
 *
 * [fadeAmount] is the share of its alpha spent over the whole run, so `1` fades
 * to nothing and `0` holds. Clamped, so no configuration can push an effect past
 * opaque or into negative alpha.
 */
internal fun pulseAlphaAt(progress: Float, intensity: Float, fadeAmount: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return (intensity * (1f - fadeAmount.coerceIn(0f, 1f) * t)).coerceIn(0f, 1f)
}

/**
 * How much extra glow the arrow under a pulse gets right now.
 *
 * Instant attack and a squared decay, so the brightening lands on the same frame
 * as the tap and is gone well before the arrow has finished leaving the board.
 * Zero outside a pulse, so a resting arrow is never drawn differently.
 */
internal fun arrowGlowBoostAt(progress: Float, boost: Float): Float {
    val remaining = 1f - progress.coerceIn(0f, 1f)
    return (boost * remaining * remaining).coerceAtLeast(0f)
}

/**
 * How far a layer that expands outward has opened at [progress], easing applied
 * by the caller.
 *
 * Shared by the pulse's own layers and by every energy ring and spark, so all of
 * them open from the same fraction of their own reach instead of each inventing
 * a way to start.
 */
internal fun growthAt(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return StartRadiusFraction + (1f - StartRadiusFraction) * smoothRamp(0f, 1f, t)
}

/** Whole pulse, in milliseconds. Short enough that gameplay never waits on it. */
internal const val DefaultDurationMillis = 110

private const val DefaultPulseRadiusMultiplier = 0.95f
private const val DefaultGlowRadiusMultiplier = 1.45f
private const val DefaultRingRadiusMultiplier = 0.72f
private const val DefaultRingThicknessMultiplier = 0.055f
private const val DefaultCoreRadiusMultiplier = 0.34f
private const val DefaultGlowIntensity = 0.50f
private const val DefaultRingIntensity = 0.34f
private const val DefaultCoreIntensity = 0.40f
private const val DefaultArrowGlowBoost = 0.34f

/** Fraction of its widest size the pulse already has on the frame it starts. */
internal const val StartRadiusFraction = 0.22f

/** Share of its starting thickness the ring gives up as it travels. */
private const val RingThinBy = 0.60f

/** Floor on the ring, as a fraction of the arrow's size, so it never vanishes. */
private const val MinRingThickness = 0.012f

/** Concentric discs used to build the soft falloff of each glow layer. */
private const val GlowLayers = 5

/** Fallback floor so an outermost disc still contributes a little. */
private const val LayerFloor = 0.08f

/** Normalises the composited discs back to the alpha they were asked for. */
private const val GlowLayerGain = 0.44f

/** How much of the pulse the wide ambient glow carries, against the front. */
private const val HaloGain = 0.55f

/** How much of the ring's alpha its wider, fainter skirt carries. */
private const val RingSkirtGain = 0.38f

/** How much wider the ring's skirt is than the ring's own stroke. */
private const val RingSkirtWidth = 2.6f

/** Alphas under this are not worth a draw call. */
internal const val AlphaFloor = 0.004f
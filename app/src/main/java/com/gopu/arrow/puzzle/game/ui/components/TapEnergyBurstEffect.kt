package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Every dimension, intensity and count knob of the energy a launch throws off.
 *
 * As with the glow pulse, not one of these is a pixel value: the ring reach, the
 * spark reach, the spark thickness and the head dot are all the tapped arrow's
 * own rendered size times a multiplier. The spark count is the only count here,
 * and it is a count of marks rather than a length, so it needs no scaling.
 *
 * The run is timed by the clock the owning [LaunchAnimation] keeps, so this
 * deliberately has no duration of its own: one clock drives the whole style and
 * the two halves can never drift apart.
 */
internal data class TapEnergyBurstConfig(
    /** Curve the rings and the sparks both run on. */
    val expansionEasing: Easing = FastOutSlowInEasing,

    /** How many sparks fly. Evenly spread, then scattered off the even angles. */
    val particleCount: Int = DefaultParticleCount,

    /** Thickness of a spark, as a fraction of the arrow's size. */
    val particleRadiusMultiplier: Float = DefaultParticleRadiusMultiplier,

    /** How far the quickest spark flies, as a fraction of the arrow's size. */
    val particleReachMultiplier: Float = DefaultParticleReachMultiplier,

    /** Length of the streak a spark drags, as a fraction of the arrow's size. */
    val particleTrailMultiplier: Float = DefaultParticleTrailMultiplier,

    /** Alpha a spark reaches on the frame it leaves. */
    val particleIntensity: Float = DefaultParticleIntensity,

    /** Alpha of the bright dot on the head of a spark. Zero hides the heads. */
    val particleHeadIntensity: Float = DefaultParticleHeadIntensity,

    /** How far the energy ring travels, as a fraction of the arrow's size. */
    val ringRadiusMultiplier: Float = DefaultRingRadiusMultiplier,

    /** Thickness the energy ring starts at, as a fraction of the arrow's size. */
    val ringThicknessMultiplier: Float = DefaultRingThicknessMultiplier,

    /** Alpha the energy ring reaches on the frame it leaves. */
    val ringIntensity: Float = DefaultRingIntensity,

    /** How many energy rings trail each other. One is a single shockwave. */
    val ringCount: Int = 1,

    /** How far behind the ring ahead of it each following ring starts. */
    val ringLag: Float = DefaultRingLag,

    /** How much of its alpha each layer has lost by the end of the run. */
    val fadeAmount: Float = 1f
)

/**
 * The sparks and shock rings a launch throws off the tapped arrow.
 *
 * A second reusable effect, structured exactly like [TapGlowPulseEffect]: it owns
 * the geometry and colour of the arrow it was armed on, takes its shape from a
 * [TapEnergyBurstConfig] per draw, and is re-armed in place rather than rebuilt.
 * The two effects are independent, so a style can play either, both or neither.
 *
 * The spark scatter is derived from each spark's index rather than from a random
 * number, so the burst is scattered — never a perfect starburst — while costing
 * a sine and a floor per spark and allocating nothing at all, on any frame.
 */
internal class TapEnergyBurstEffect {

    private var center = Offset.Zero
    private var arrowSize = 0f
    private var color = NeonCyan

    /**
     * Points this effect at an arrow, ready for the next [draw]. The arrow's own
     * size and colour are all the burst needs to be measured and tinted against.
     */
    fun arm(center: Offset, arrowSize: Float, color: Color) {
        this.center = center
        this.arrowSize = arrowSize
        this.color = color
    }

    /**
     * Draws the burst at [progress], 0 on the frame the arrow is tapped and 1 when
     * the run is over. Does nothing once it is finished or once the style it was
     * given asks for neither sparks nor rings.
     */
    fun draw(scope: DrawScope, progress: Float, config: TapEnergyBurstConfig) {
        if (arrowSize <= 0f) return

        val t = config.expansionEasing.transform(progress.coerceIn(0f, 1f))
        val hot = lerp(color, NeonCore, 0.5f)

        // Rings first and behind: they are the wave the sparks are riding out on.
        for (ring in 0 until config.ringCount) {
            val ringProgress = burstRingProgressAt(ring, t, config.ringLag)
            val alpha = pulseAlphaAt(ringProgress, config.ringIntensity, config.fadeAmount)
            if (alpha <= AlphaFloor) continue
            scope.drawCircle(
                color = lerp(color, NeonCore, 0.25f).copy(alpha = alpha),
                radius = burstRingRadiusAt(
                    arrowSize = arrowSize,
                    ringIndex = ring,
                    progress = t,
                    multiplier = config.ringRadiusMultiplier,
                    ringLag = config.ringLag
                ),
                center = center,
                style = Stroke(width = ringThicknessAt(arrowSize, ringProgress, config.ringThicknessMultiplier)),
                blendMode = BlendMode.Plus
            )
        }

        // Sparks, each a short streak with a bright head, running outward.
        val sparkLength = particleLengthAt(arrowSize, t, config.particleTrailMultiplier)
        val sparkWidth = config.particleRadiusMultiplier * arrowSize * particleStrokeScaleAt(t)
        val headAlpha = pulseAlphaAt(t, config.particleHeadIntensity, config.fadeAmount)

        for (index in 0 until config.particleCount) {
            val alpha = particleAlphaAt(index, t, config.particleIntensity, config.fadeAmount)
            if (alpha <= AlphaFloor) continue

            val reach = particleReachAt(arrowSize, index, t, config.particleReachMultiplier)
            if (reach <= 0f) continue

            val angle = particleAngleAt(index, config.particleCount)
            val dx = cos(angle)
            val dy = sin(angle)
            val head = Offset(center.x + dx * reach, center.y + dy * reach)
            val tail = Offset(center.x + dx * (reach - sparkLength), center.y + dy * (reach - sparkLength))

            scope.drawLine(
                color = hot.copy(alpha = alpha),
                start = tail,
                end = head,
                strokeWidth = sparkWidth,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )

            if (headAlpha > AlphaFloor) {
                scope.drawCircle(
                    color = NeonCore.copy(alpha = alpha * headAlpha),
                    radius = sparkWidth * ParticleHeadRadius,
                    center = head,
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

/**
 * The direction spark [index] leaves in, in radians clockwise from +x.
 *
 * The [count] sparks start evenly spread so no direction is left bare, and each
 * is then pushed off its even angle by a stable hash of its index. The hash is
 * what makes the burst scattered rather than a symmetrical starburst while
 * staying pure: the same spark leaves the same way on every frame and every run.
 */
internal fun particleAngleAt(index: Int, count: Int): Float {
    val even = if (count <= 1) 0f else TWO_PI * index / count
    val scatter = (hashUnit(index) - 0.5f) * ParticleScatterRadians
    return even + scatter
}

/**
 * How fast spark [index] travels relative to the others.
 *
 * A stable spread above one, so the furthest spark of a burst is genuinely
 * further than the slowest and the burst does not read as one solid ring.
 */
internal fun particleSpeedAt(index: Int): Float =
    ParticleSpeedFloor + (1f - ParticleSpeedFloor) * hashUnit(index + SpeedHashOffset)

/**
 * How bright spark [index] is at full strength relative to the others, so the
 * burst has depth instead of every mark carrying the same weight.
 */
internal fun particleWeightAt(index: Int): Float =
    ParticleWeightFloor + (1f - ParticleWeightFloor) * hashUnit(index + WeightHashOffset)

/**
 * How far along its path a spark is at [progress]: still on the arrow at the
 * start, at its full reach by the end.
 */
internal fun particleTravelAt(progress: Float): Float = progress.coerceIn(0f, 1f)

/**
 * How far spark [index] is from the centre at [progress], in the arrow's own
 * units. Scaled by the arrow, never by the screen.
 */
internal fun particleReachAt(
    arrowSize: Float,
    index: Int,
    progress: Float,
    reachMultiplier: Float
): Float =
    arrowSize * reachMultiplier * particleSpeedAt(index) * particleTravelAt(progress)

/**
 * The streak a spark drags at [progress], in the arrow's own units.
 *
 * It shortens as the spark goes, so a spark reads as something being left behind
 * rather than a line being dragged along.
 */
internal fun particleLengthAt(arrowSize: Float, progress: Float, trailMultiplier: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val length = arrowSize * trailMultiplier * (1f - SparkTrailShorten * t)
    return maxOf(length, 0f)
}

/**
 * How thick a spark is at [progress], as a fraction of its starting thickness.
 * It thins on the way out, like the rings.
 */
internal fun particleStrokeScaleAt(progress: Float): Float =
    (1f - SparkStrokeThinBy * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)

/**
 * How visible spark [index] is at [progress]: its own share of the intensity,
 * fading over the run.
 */
internal fun particleAlphaAt(
    index: Int,
    progress: Float,
    intensity: Float,
    fadeAmount: Float
): Float = pulseAlphaAt(progress, intensity * particleWeightAt(index), fadeAmount)

/**
 * How far behind the ring ahead of it ring [ringIndex] is running, so a trail of
 * rings reads as one wave rather than several unrelated circles.
 */
internal fun burstRingProgressAt(ringIndex: Int, progress: Float, ringLag: Float): Float =
    (progress - ringIndex * ringLag.coerceAtLeast(0f)).coerceIn(0f, 1f)

/**
 * How wide energy ring [ringIndex] is at [progress], in the arrow's own units.
 * Each ring uses the same opening curve as the pulse, lagged, so the two
 * treatments share one feel.
 */
internal fun burstRingRadiusAt(
    arrowSize: Float,
    ringIndex: Int,
    progress: Float,
    multiplier: Float,
    ringLag: Float
): Float = arrowSize * multiplier * growthAt(burstRingProgressAt(ringIndex, progress, ringLag))

/** A stable 0 to 1 value for [index], from a sine that never repeats in range. */
private fun hashUnit(index: Int): Float {
    val noise = sin((index + 1) * HashFrequency) * HashScale
    return noise - floor(noise)
}

private const val DefaultParticleCount = 8
private const val DefaultParticleRadiusMultiplier = 0.055f
private const val DefaultParticleReachMultiplier = 1.25f
private const val DefaultParticleTrailMultiplier = 0.55f
private const val DefaultParticleIntensity = 0.55f
private const val DefaultParticleHeadIntensity = 0.60f
private const val DefaultRingRadiusMultiplier = 1.15f
private const val DefaultRingThicknessMultiplier = 0.035f
private const val DefaultRingIntensity = 0.40f
private const val DefaultRingLag = 0.32f

/** Radians each spark is pushed off its even angle, either way. */
private const val ParticleScatterRadians = 0.9f

/** Spread of the per-spark speed multiplier. */
private const val ParticleSpeedFloor = 0.72f
private const val SpeedHashOffset = 977

/** Spread of the per-spark brightness multiplier. */
private const val ParticleWeightFloor = 0.6f
private const val WeightHashOffset = 613

/** Share of a spark's streak length it has given up by the end of the run. */
private const val SparkTrailShorten = 0.45f

/** Share of a spark's thickness it has given up by the end of the run. */
private const val SparkStrokeThinBy = 0.5f

/** Head dot radius as a fraction of the spark's own thickness. */
private const val ParticleHeadRadius = 1.8f

private const val TWO_PI = 6.2831855f

/** Sine hash constants: a frequency and scale whose fractional parts spread out. */
private const val HashFrequency = 12.9898f
private const val HashScale = 43758.547f
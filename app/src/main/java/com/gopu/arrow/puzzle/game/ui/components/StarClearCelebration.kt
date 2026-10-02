package com.gopu.arrow.puzzle.game.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.ui.theme.FlameOrange
import com.gopu.arrow.puzzle.game.ui.theme.Gold
import com.gopu.arrow.puzzle.game.ui.theme.NeonAmber
import com.gopu.arrow.puzzle.game.ui.theme.NeonBlue
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.NeonGreen
import com.gopu.arrow.puzzle.game.ui.theme.NeonLime
import com.gopu.arrow.puzzle.game.ui.theme.NeonMagenta
import com.gopu.arrow.puzzle.game.ui.theme.NeonOrange
import com.gopu.arrow.puzzle.game.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/*
 * ---------------------------------------------------------------------------
 * LEVEL CLEAR CELEBRATION — FIREWORKS + RIBBONS
 * ---------------------------------------------------------------------------
 *
 * A pure presentation layer. It reads the star score the level already earned
 * and plays the matching celebration on a canvas of its own, so gameplay,
 * geometry, scoring and layout are all left exactly as they were.
 *
 * The spectacle is fireworks, not gameplay pieces. Nothing here draws an arrow.
 * A shell is fired from off the bottom edge, arcs up, and bursts into a white
 * core, a fan of tapered rays and a shower of drag-slowed grains. Curved glowing
 * ribbons sweep the screen in front of and behind those bursts, and stars,
 * confetti strips and fine glitter rain down over the last second.
 *
 * The budget is what keeps it readable rather than merely busy. The whole effect
 * lives on one canvas driven by one clock, so a frame costs a few hundred simple
 * draws and never a recomposition. Every particle is closed form: its position is
 * a pure function of the timeline, so nothing accumulates error, the layout is
 * reproducible from its seed, and the effect can be scrubbed backwards without
 * drifting.
 */

/** How long the whole-screen flash burns for at the very top of the timeline. */
internal const val FlashSeconds = 0.34f

/** Length of the closing fade, so the effect never hard cuts into the card. */
internal const val ExitFadeSeconds = 0.28f

/** Samples along one ribbon: enough for a smooth sweep, no visible kinks. */
private const val RibbonSegments = 20

/** How many width steps a ribbon is broken into, which is what makes it taper. */
private const val RibbonSlices = 5

/** Sparks trailing behind a ribbon's head. */
private const val RibbonTailCount = 8

/** Points sampled back along a ribbon to light its tail sparks. */
private const val RibbonTailStep = 0.055f

private const val PIECE_STAR = 0
private const val PIECE_DOT = 1
private const val PIECE_STRIP = 2

private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun easeOutCubic(t: Float): Float {
    val x = 1f - t.coerceIn(0f, 1f)
    return 1f - x * x * x
}

/** Overshoots a little and settles, so growing text lands with a snap. */
private fun easeOutBack(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val c1 = 1.20f
    val c3 = c1 + 1f
    val p = x - 1f
    return 1f + c3 * p * p * p + c1 * p * p
}

/** A settled random value in [from, until), so the layout of a celebration is
 *  reproducible instead of jittering from one run to the next. */
private fun Random.between(from: Float, until: Float): Float =
    nextDouble(from.toDouble(), until.toDouble()).toFloat()

/** Distance travelled by something pushed at a constant speed that the air is
 *  eating away at. Approaches a fixed ceiling, which is what makes every spark
 *  ease to a halt instead of sliding off the screen forever. */
internal fun dragTravel(t: Float, drag: Float): Float =
    if (drag < 0.0001f) {
        t
    } else {
        ((1.0 - exp(-(drag * t).toDouble())) / drag.toDouble()).toFloat()
    }

/** A five point star of unit radius, drawn from a single reused path. */
private fun unitStarPath(): Path {
    val path = Path()
    val spikes = 5
    for (index in 0 until spikes * 2) {
        val radius = if (index % 2 == 0) 1f else 0.44f
        val angle = -PI / 2.0 + index * PI / spikes
        val x = (radius * cos(angle)).toFloat()
        val y = (radius * sin(angle)).toFloat()
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Every colour a shell, ribbon or falling piece is allowed to wear. Nothing in
 *  the celebration invents a new colour, so the effect stays inside the game's
 *  palette and never clashes with the board it is drawn over. */
private val CelebrationColors = listOf(
    NeonCyan,
    NeonMagenta,
    NeonLime,
    NeonAmber,
    NeonViolet,
    NeonGreen,
    NeonOrange,
    NeonBlue,
    FlameOrange
)

/* ---------------------------------------------------------------------------
 * THE TIMELINE
 *
 * These curves hold the actual feel of the celebration, so they are kept free of
 * any drawing or state. They are also the part the unit tests pin down.
 * ------------------------------------------------------------------------ */

/** How far a ray has shot out, as a fraction of its own length. */
internal fun burstRayLengthAt(age: Float, life: Float): Float {
    if (age <= 0f) return 0f
    return easeOutCubic(age / max(1e-4f, life * 0.34f))
}

/** A ray's opacity: a beat to catch the eye, then a squared falloff to nothing. */
internal fun burstRayAlphaAt(age: Float, life: Float): Float {
    if (age <= 0f || age >= life) return 0f
    val rise = smoothStep(age / max(1e-4f, life * 0.06f))
    val q = ((age - life * 0.22f) / max(1e-4f, life * 0.78f)).coerceIn(0f, 1f)
    return rise * (1f - q) * (1f - q)
}

/** The white core of a shell, in [0, 1]. Sharp attack, quick decay. */
internal fun burstFlashAlphaAt(age: Float, life: Float): Float {
    if (age < 0f || age > life * 0.45f) return 0f
    val rise = smoothStep(age / max(1e-4f, life * 0.05f))
    val fall = 1f - smoothStep((age - life * 0.06f) / max(1e-4f, life * 0.34f))
    return rise * fall
}

/** How far a grain has fallen and drifted from its burst, in stage pixels. */
internal fun burstGrainOffset(
    age: Float,
    life: Float,
    speed: Float,
    angle: Float,
    drag: Float,
    gravity: Float,
    out: FloatArray
) {
    val t = age.coerceIn(0f, life)
    val travel = dragTravel(t, drag)
    out[0] = cos(angle) * speed * travel
    out[1] = sin(angle) * speed * travel + 0.5f * gravity * t * t
}

/** How far a ribbon's head has swept along its own curve, in [0, 1]. */
internal fun ribbonHeadAt(age: Float, life: Float): Float {
    if (age <= 0f) return 0f
    if (age >= life) return 1f
    return smoothStep(age / life)
}

/** A ribbon's opacity: it brightens as it enters and dissolves as it leaves. */
internal fun ribbonAlphaAt(age: Float, life: Float): Float {
    if (age <= 0f || age >= life) return 0f
    val rise = smoothStep(age / max(1e-4f, life * 0.12f))
    val fall = 1f - smoothStep(
        ((age - life * 0.58f) / max(1e-4f, life * 0.42f)).coerceIn(0f, 1f)
    )
    return rise * fall
}

/** The screen flash that covers the very first frame of the celebration. */
internal fun screenFlashAlphaAt(time: Float): Float {
    val rise = smoothStep(time / 0.05f)
    val fall = 1f - smoothStep(((time - 0.06f) / 0.26f).coerceIn(0f, 1f))
    return rise * fall
}

/** The closing fade applied to every drawn layer, so the celebration always
 *  dissolves before the result card is handed the screen. */
internal fun celebrationFadeAt(time: Float, duration: Float): Float =
    1f - smoothStep((time - (duration - ExitFadeSeconds)) / ExitFadeSeconds)

/** A falling piece's opacity: it appears, then fades as it drops. */
internal fun fallAlphaAt(time: Float, delay: Float, life: Float): Float {
    val age = time - delay
    if (age < 0f || age > life) return 0f
    val rise = smoothStep(age / max(1e-4f, life * 0.08f))
    val q = age / life
    return rise * (1f - q * q)
}

/**
 * How much celebration a finished level earns. The tier is read straight from
 * the star score the level already produced, so the spectacle is a direct read of
 * how well the player did and is never rolled at random.
 *
 * Every tier keeps the same wall clock budget this celebration has always had.
 * The premium tier spends it on more shells, more ribbons and a heavier rain; a
 * one star clear spends it on three shells and stays snappy.
 */
internal enum class ClearTier(
    val anchorIndices: IntArray,
    val launchFractions: FloatArray,
    val ribbonPairs: Int,
    val raysPerBurst: Int,
    val grainsPerBurst: Int,
    val confettiCount: Int,
    val glitterCount: Int,
    val burstSpread: Float,
    val burstSpeed: Float,
    val ribbonWidth: Float,
    val bannerColors: List<Color>,
    val flashGain: Float,
    val showPerfect: Boolean,
    val perfectAt: Float,
    val clearAt: Float,
    val durationMillis: Int
) {
    /** "I completed it." Three shells and one ribbon pair, then out. */
    SIMPLE(
        anchorIndices = intArrayOf(0, 1, 4),
        launchFractions = floatArrayOf(0.07f, 0.20f, 0.33f),
        ribbonPairs = 1,
        raysPerBurst = 12,
        grainsPerBurst = 5,
        confettiCount = 12,
        glitterCount = 14,
        burstSpread = 0.36f,
        burstSpeed = 0.95f,
        ribbonWidth = 0.078f,
        bannerColors = listOf(NeonLime, NeonCore, NeonGreen),
        flashGain = 0.55f,
        showPerfect = false,
        perfectAt = 1f,
        clearAt = 0.40f,
        durationMillis = 1700
    ),

    /** "Nice performance." Five shells, two ribbon pairs, a real shower. */
    GOOD(
        anchorIndices = intArrayOf(0, 1, 2, 3, 4),
        launchFractions = floatArrayOf(0.06f, 0.14f, 0.22f, 0.30f, 0.38f),
        ribbonPairs = 2,
        raysPerBurst = 16,
        grainsPerBurst = 7,
        confettiCount = 20,
        glitterCount = 20,
        burstSpread = 0.42f,
        burstSpeed = 1.05f,
        ribbonWidth = 0.082f,
        bannerColors = listOf(NeonCyan, NeonCore, NeonViolet),
        flashGain = 0.78f,
        showPerfect = false,
        perfectAt = 1f,
        clearAt = 0.42f,
        durationMillis = 2400
    ),

    /** "Perfect! That was special." Every shell, every ribbon, the full rain. */
    PERFECT(
        anchorIndices = intArrayOf(0, 1, 2, 3, 4, 5, 6),
        launchFractions = floatArrayOf(0.05f, 0.11f, 0.17f, 0.23f, 0.29f, 0.35f, 0.41f),
        ribbonPairs = 3,
        raysPerBurst = 20,
        grainsPerBurst = 9,
        confettiCount = 28,
        glitterCount = 28,
        burstSpread = 0.47f,
        burstSpeed = 1.18f,
        ribbonWidth = 0.086f,
        bannerColors = listOf(Gold, NeonCore, FlameOrange),
        flashGain = 1f,
        showPerfect = true,
        perfectAt = 0.30f,
        clearAt = 0.58f,
        durationMillis = 3000
    );

    companion object {
        /** One star is a simple clear, two a good clear, three a perfect clear. */
        fun of(stars: Int): ClearTier = when (stars) {
            1 -> SIMPLE
            2 -> GOOD
            else -> PERFECT
        }
    }
}

/** Where a shell is fired from and where it goes off, all as fractions of the
 *  stage. Two launch off the bottom corners, two from lower and wider, one
 *  straight up the middle behind the banner, and two late extras for the premium
 *  tier so the sky fills in behind the text instead of only around it. */
private class ShellAnchor(
    val startX: Float,
    val startY: Float,
    val controlX: Float,
    val controlY: Float,
    val burstX: Float,
    val burstY: Float
)

private val ShellAnchors = arrayOf(
    ShellAnchor(0.05f, 1.08f, 0.00f, 0.60f, 0.28f, 0.38f),
    ShellAnchor(0.95f, 1.08f, 1.00f, 0.60f, 0.72f, 0.38f),
    ShellAnchor(0.12f, 1.02f, 0.03f, 0.66f, 0.16f, 0.18f),
    ShellAnchor(0.88f, 1.02f, 0.97f, 0.66f, 0.86f, 0.18f),
    ShellAnchor(0.50f, 1.16f, 0.50f, 0.70f, 0.50f, 0.46f),
    ShellAnchor(0.22f, 1.12f, 0.13f, 0.78f, 0.40f, 0.12f),
    ShellAnchor(0.80f, 1.12f, 0.89f, 0.80f, 0.64f, 0.66f)
)

/** The curve one ribbon rides, again in stage fractions. Ribbons enter and leave
 *  off screen, so a sweep never appears to begin or end in mid air. */
private class RibbonAnchor(
    val startX: Float,
    val startY: Float,
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float,
    val endX: Float,
    val endY: Float,
    val major: Boolean
)

private val RibbonAnchors = arrayOf(
    RibbonAnchor(-0.10f, 0.30f, 0.28f, 0.00f, 0.72f, 0.10f, 1.10f, 0.26f, true),
    RibbonAnchor(1.10f, 0.60f, 0.74f, 0.96f, 0.28f, 0.88f, -0.10f, 0.56f, true),
    RibbonAnchor(-0.10f, 0.74f, 0.30f, 0.52f, 0.70f, 0.58f, 1.10f, 0.34f, false),
    RibbonAnchor(1.10f, 0.12f, 0.72f, -0.08f, 0.30f, 0.00f, -0.10f, 0.20f, false),
    RibbonAnchor(-0.12f, 0.46f, 0.26f, 0.88f, 0.74f, 0.94f, 1.12f, 0.50f, true),
    RibbonAnchor(1.12f, 0.86f, 0.70f, 0.30f, 0.30f, 0.26f, -0.12f, 0.78f, false)
)

/** The stage the celebration is laid out in, and every size derived from it. */
internal class CelebrationStage(
    val width: Float,
    val height: Float,
    val tier: ClearTier,
    val duration: Float
) {
    val centerX = width * 0.5f
    val centerY = height * 0.5f
    val radius = min(width, height) * 0.5f
    val groundY = height * 0.96f
    val burstSpread = radius * tier.burstSpread
    val burstSpeed = radius * tier.burstSpeed
    val ribbonWidth = radius * tier.ribbonWidth
}

/** One streak of a burst. Every ray of a shell shares the shell's colour, so a
 *  burst reads as one firework rather than as a random spray of colours. */
internal class RaySeed(
    val angle: Float,
    val lengthScale: Float,
    val screamer: Boolean
)

/** One falling ember of a burst, thrown a little faster or slower than its
 *  neighbours so the shell never opens into a perfect circle. */
internal class GrainSeed(
    val angle: Float,
    val speedScale: Float,
    val gravityScale: Float,
    val size: Float
)

/**
 * One firework: a shell that climbs, a burst that opens.
 *
 * The shell's climb is a quadratic arc from off the bottom edge to its burst
 * point, so nothing is ever fired straight up a rail, and the burst is closed
 * form: rays grow with an ease-out, grains decelerate against drag and droop
 * under gravity. The three ray paths are kept on the shell and rewound every
 * frame, so a whole burst costs three strokes and a handful of dots.
 */
internal class Firework(
    val launchAt: Float,
    val flight: Float,
    val startX: Float,
    val startY: Float,
    val controlX: Float,
    val controlY: Float,
    val burstX: Float,
    val burstY: Float,
    val life: Float,
    val color: Color,
    val spread: Float,
    val speed: Float,
    val rays: Array<RaySeed>,
    val grains: Array<GrainSeed>
) {
    /** The instant this shell goes off. */
    val burstAt: Float get() = launchAt + flight

    /** Reused every frame, so drawing a burst allocates nothing. */
    val widePath = Path()
    val midPath = Path()
    val corePath = Path()

    /** Where the climbing shell is, in stage pixels. False once it has gone off. */
    fun shellAt(time: Float, out: FloatArray): Boolean {
        if (time < launchAt || time >= burstAt) return false
        val u = ((time - launchAt) / max(1e-4f, flight)).coerceIn(0f, 1f)
        val inverse = 1f - u
        val a = inverse * inverse
        val b = 2f * inverse * u
        val c = u * u
        out[0] = a * startX + b * controlX + c * burstX
        out[1] = a * startY + b * controlY + c * burstY
        return true
    }
}

/**
 * One glowing ribbon: a cubic curve the head sweeps along, painted in stacked
 * passes so it reads as light rather than as a drawn line.
 *
 * The width steps up along the sweep, which is what gives the ribbon a taper: it
 * is thin at its tail and thickest under the head. A short fan of sparks trails
 * behind that head so the ribbon leaves something glowing in its wake.
 */
internal class Ribbon(
    val startX: Float,
    val startY: Float,
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float,
    val endX: Float,
    val endY: Float,
    val startAt: Float,
    val life: Float,
    val width: Float,
    val color: Color
) {
    val fullPath = Path()
    val slicePaths = Array(RibbonSlices) { Path() }

    /** A point on the ribbon's curve at [u], in stage pixels. */
    fun pointAt(u: Float, out: FloatArray) {
        val x = u.coerceIn(0f, 1f)
        val inverse = 1f - x
        val a = inverse * inverse * inverse
        val b = 3f * inverse * inverse * x
        val c = 3f * inverse * x * x
        val d = x * x * x
        out[0] = a * startX + b * c1x + c * c2x + d * endX
        out[1] = a * startY + b * c1y + c * c2y + d * endY
    }
}

/**
 * One star, strip or dot of the falling confetti, plus the fine glitter that
 * drifts for the rest of the celebration.
 *
 * The wobble is what sells it as paper rather than as a particle: it is a slow
 * sine across the horizontal, layered on a gravity accelerated fall, so pieces
 * drift sideways as they drop instead of dropping straight.
 */
internal class CelebrationPiece(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val drag: Float,
    val gravity: Float,
    val delay: Float,
    val life: Float,
    val size: Float,
    val kind: Int,
    val spin: Float,
    val phase: Float,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val color: Color
) {
    fun alphaAt(time: Float): Float = fallAlphaAt(time, delay, life)

    fun positionAt(time: Float, out: FloatArray) {
        val age = (time - delay).coerceIn(0f, life)
        val travel = dragTravel(age, drag)
        out[0] = x + vx * travel + wobbleAmp * sin(wobbleFreq * age + phase)
        out[1] = y + vy * travel + 0.5f * gravity * age * age
    }

    fun spinAt(time: Float): Float = spin * ((time - delay).coerceIn(0f, life)) + phase
}

/** Everything the celebration draws, built once per run. */
internal class CelebrationModel(
    val stage: CelebrationStage,
    val tier: ClearTier,
    val shells: List<Firework>,
    val ribbons: List<Ribbon>,
    val pieces: List<CelebrationPiece>,
    val glitter: List<CelebrationPiece>,
    val starPath: Path,
    val bannerBloom: Brush,
    val bannerY: Float,
    val duration: Float
) {
    companion object {

        fun build(width: Float, height: Float, stars: Int): CelebrationModel {
            val tier = ClearTier.of(stars)
            val duration = tier.durationMillis / 1000f
            val stage = CelebrationStage(width, height, tier, duration)
            // Seeded per run, so the layout is settled rather than jittery and no
            // random number is generated inside a frame.
            val random = Random(stars * 7919 + width.toInt() * 31 + height.toInt())

            return CelebrationModel(
                stage = stage,
                tier = tier,
                shells = buildShells(tier, stage, random),
                ribbons = buildRibbons(tier, stage),
                pieces = buildPieces(tier, stage, random),
                glitter = buildGlitter(tier, stage, random),
                starPath = unitStarPath(),
                bannerBloom = Brush.radialGradient(
                    0.00f to NeonCore.copy(alpha = 0.26f),
                    0.34f to NeonAmber.copy(alpha = 0.13f),
                    1.00f to Color.Transparent,
                    center = Offset(stage.centerX, stage.centerY - stage.radius * 0.14f),
                    radius = stage.radius * 1.25f
                ),
                bannerY = stage.radius * 0.86f,
                duration = duration
            )
        }

        private fun buildShells(
            tier: ClearTier,
            stage: CelebrationStage,
            random: Random
        ): List<Firework> {
            val shells = ArrayList<Firework>(tier.anchorIndices.size)
            val flight = max(0.22f, stage.duration * 0.16f)
            val life = min(0.95f, max(0.55f, stage.duration * 0.45f))

            for (index in tier.anchorIndices.indices) {
                val anchor = ShellAnchors[tier.anchorIndices[index]]
                val launchAt = stage.duration * tier.launchFractions[index]

                val rays = Array(tier.raysPerBurst) { ray ->
                    // Evenly spread with a little jitter, so the fan never looks
                    // like a compass rose.
                    val jitter = random.nextDouble(-0.13, 0.13).toFloat()
                    val angle = (ray * 2f * PI.toFloat() / tier.raysPerBurst) + jitter
                    // One in four is a screamer: longer, and it sets the width of
                    // the whole fan, which is most of what breaks the symmetry.
                    val screamer = ray % 4 == 0
                    RaySeed(
                        angle = angle,
                        lengthScale = if (screamer) {
                            random.between(1.10f, 1.34f)
                        } else {
                            random.between(0.72f, 1.02f)
                        },
                        screamer = screamer
                    )
                }

                val grains = Array(tier.grainsPerBurst) { grain ->
                    val jitter = random.nextDouble(-0.26, 0.26).toFloat()
                    GrainSeed(
                        angle = (grain * 2f * PI.toFloat() / tier.grainsPerBurst) + jitter,
                        speedScale = random.between(0.55f, 1.05f),
                        gravityScale = random.between(0.55f, 1.35f),
                        size = random.between(0.6f, 1.15f)
                    )
                }

                shells += Firework(
                    launchAt = launchAt,
                    flight = flight,
                    startX = anchor.startX * stage.width,
                    startY = anchor.startY * stage.height,
                    controlX = anchor.controlX * stage.width,
                    controlY = anchor.controlY * stage.height,
                    burstX = anchor.burstX * stage.width,
                    burstY = anchor.burstY * stage.height,
                    life = life,
                    color = CelebrationColors[(index * 2 + 1) % CelebrationColors.size],
                    spread = stage.burstSpread * random.between(0.86f, 1.12f),
                    speed = stage.burstSpeed * random.between(0.88f, 1.14f),
                    rays = rays,
                    grains = grains
                )
            }

            return shells
        }

        private fun buildRibbons(tier: ClearTier, stage: CelebrationStage): List<Ribbon> {
            val ribbons = ArrayList<Ribbon>(tier.ribbonPairs * 2)
            val life = min(1.10f, max(0.50f, stage.duration * 0.42f))

            for (pair in 0 until tier.ribbonPairs) {
                for (side in 0 until 2) {
                    val index = pair * 2 + side
                    val anchor = RibbonAnchors[index % RibbonAnchors.size]
                    val major = anchor.major
                    ribbons += Ribbon(
                        startX = anchor.startX * stage.width,
                        startY = anchor.startY * stage.height,
                        c1x = anchor.c1x * stage.width,
                        c1y = anchor.c1y * stage.height,
                        c2x = anchor.c2x * stage.width,
                        c2y = anchor.c2y * stage.height,
                        endX = anchor.endX * stage.width,
                        endY = anchor.endY * stage.height,
                        startAt = stage.duration * (0.06f + 0.16f * pair),
                        life = life,
                        width = stage.ribbonWidth * if (major) 1f else 0.62f,
                        color = CelebrationColors[(index * 3 + 2) % CelebrationColors.size]
                    )
                }
            }

            return ribbons
        }

        private fun buildPieces(
            tier: ClearTier,
            stage: CelebrationStage,
            random: Random
        ): List<CelebrationPiece> {
            val pieces = ArrayList<CelebrationPiece>(tier.confettiCount)
            for (index in 0 until tier.confettiCount) {
                val roll = random.nextFloat()
                val kind = if (roll < 0.45f) {
                    PIECE_STAR
                } else if (roll < 0.70f) {
                    PIECE_STRIP
                } else {
                    PIECE_DOT
                }
                pieces += CelebrationPiece(
                    x = random.between(0.04f, 0.96f) * stage.width,
                    y = random.between(-0.12f, 0.20f) * stage.height,
                    vx = random.between(-0.16f, 0.16f) * stage.radius,
                    vy = random.between(0.10f, 0.26f) * stage.radius,
                    drag = random.between(0.7f, 1.3f),
                    gravity = stage.radius * random.between(1.1f, 1.9f),
                    // Spread over the middle of the celebration and kept inside
                    // it, so the last piece is still falling when the card
                    // appears rather than being cut in half by the fade.
                    delay = stage.duration * 0.30f + index * (stage.duration * 0.006f),
                    life = stage.duration * random.between(0.40f, 0.52f),
                    size = stage.radius * random.between(0.018f, 0.034f),
                    kind = kind,
                    spin = random.between(-3.4f, 3.4f),
                    phase = random.between(0f, 2f * PI.toFloat()),
                    wobbleAmp = stage.radius * random.between(0.020f, 0.060f),
                    wobbleFreq = random.between(4.4f, 7.6f),
                    color = CelebrationColors[(index * 5 + 3) % CelebrationColors.size]
                )
            }
            return pieces
        }

        private fun buildGlitter(
            tier: ClearTier,
            stage: CelebrationStage,
            random: Random
        ): List<CelebrationPiece> {
            val glitter = ArrayList<CelebrationPiece>(tier.glitterCount)
            // Laid in across the tail of the celebration and short lived, so the
            // drift is still going when everything else has faded out.
            val firstAt = stage.duration * 0.30f
            val step = stage.duration * 0.006f
            val life = stage.duration * 0.38f
            for (index in 0 until tier.glitterCount) {
                glitter += CelebrationPiece(
                    x = random.nextFloat() * stage.width,
                    y = random.between(-0.20f, 0.05f) * stage.height,
                    vx = random.between(-0.05f, 0.05f) * stage.radius,
                    vy = random.between(0.04f, 0.13f) * stage.radius,
                    drag = 1.4f,
                    gravity = stage.radius * 0.22f,
                    delay = firstAt + index * step,
                    life = life,
                    size = stage.radius * random.between(0.004f, 0.009f),
                    kind = PIECE_DOT,
                    spin = 0f,
                    phase = 0f,
                    wobbleAmp = stage.radius * random.between(0.010f, 0.030f),
                    wobbleFreq = random.between(2.4f, 4.2f),
                    color = lerp(
                        CelebrationColors[(index * 4) % CelebrationColors.size],
                        NeonCore,
                        0.45f
                    )
                )
            }
            return glitter
        }
    }
}

/*
 * ---------------------------------------------------------------------------
 * THE CELEBRATION
 * ---------------------------------------------------------------------------
 */

/**
 * The level clear celebration, driven entirely by the star score.
 *
 * One star earns three shells and a single ribbon pair; two stars add a second
 * pair and a heavier shower; three stars add every shell, three ribbon pairs and
 * a brief PERFECT! before the LEVEL CLEAR! banner.
 *
 * The composable owns the only clock in the effect and runs the timeline and the
 * closing fade itself, so leaving the screen cancels all of it in one place, no
 * animation can outlive it, and the result card can be handed a plain "I am
 * done" callback with no transition state of its own.
 */
@Composable
fun StarClearCelebration(
    stars: Int,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val tier = ClearTier.of(stars)
    val reduceMotion = rememberSystemReduceMotion()
    // With system animations off, the fireworks and ribbons are skipped entirely
    // and the celebration collapses to the banner and one soft pulse, so nothing
    // moves that the player has asked not to see move.
    val durationMillis = if (reduceMotion) 900 else tier.durationMillis
    val perfectAt = if (reduceMotion) 0.10f else tier.perfectAt
    val clearAt = if (reduceMotion) 0.22f else tier.clearAt

    val clock = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(stars) {
        clock.snapTo(0f)
        clock.animateTo(1f, tween(durationMillis, easing = LinearEasing))
        fade.animateTo(0f, tween(160, easing = LinearEasing))
        onFinished()
    }

    BoxWithConstraints(
        modifier = modifier.graphicsLayer { alpha = fade.value }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val model = remember(widthPx, heightPx, stars) {
            if (widthPx > 0f && heightPx > 0f) {
                CelebrationModel.build(widthPx, heightPx, stars)
            } else {
                null
            }
        }

        if (model != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCelebration(model, clock.value.coerceIn(0f, 1f) * model.duration, reduceMotion)
            }
            CelebrationBanner(
                tier = tier,
                bannerY = model.bannerY,
                perfectAt = perfectAt,
                clearAt = clearAt,
                clock = clock
            )
        }
    }
}

/** PERFECT! on three stars, then LEVEL CLEAR! on every tier. */
@Composable
private fun CelebrationBanner(
    tier: ClearTier,
    bannerY: Float,
    perfectAt: Float,
    clearAt: Float,
    clock: Animatable<Float, *>
) {
    // Sits below the middle of the board, clear of the burst line and of the HUD
    // no matter which way the shells are placed.
    val offset = with(LocalDensity.current) { bannerY.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (tier.showPerfect) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = offset)
                    .graphicsLayer {
                        val t = clock.value
                        alpha = bannerAlpha(
                            t = t,
                            start = perfectAt,
                            fadeIn = 0.05f,
                            fadeOutStart = clearAt - 0.07f,
                            fadeOutEnd = clearAt
                        )
                        val rise = ((t - perfectAt) / 0.14f).coerceIn(0f, 1f)
                        val s = 0.86f + 0.14f * easeOutBack(rise)
                        // A slow breath once it has landed, so the banner never
                        // sits completely still under the fireworks.
                        val beat = 1f + 0.020f * sin(t * 5.4f)
                        scaleX = s * beat
                        scaleY = s * beat
                    }
            ) {
                BannerText(
                    text = "PERFECT!",
                    colors = listOf(NeonCyan, NeonCore, NeonViolet),
                    glow = NeonCyan
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = offset)
                .graphicsLayer {
                    val t = clock.value
                    alpha = bannerAlpha(
                        t = t,
                        start = clearAt,
                        fadeIn = 0.06f,
                        fadeOutStart = 2f,
                        fadeOutEnd = 2.1f
                    )
                    val rise = ((t - clearAt) / 0.16f).coerceIn(0f, 1f)
                    val s = 0.86f + 0.14f * easeOutBack(rise)
                    val beat = 1f + 0.026f * sin(t * 4.2f)
                    scaleX = s * beat
                    scaleY = s * beat
                }
        ) {
            BannerText(
                text = "LEVEL CLEAR!",
                colors = tier.bannerColors,
                glow = tier.bannerColors.first()
            )
        }
    }
}

/** The banner's own gradient, so the text itself is two toned rather than a flat
 *  block of one colour. */
private class BannerBrush(private val colors: List<Color>) : ShaderBrush() {
    override fun createShader(size: Size): Shader = LinearGradientShader(
        from = Offset.Zero,
        to = Offset(size.width, size.height),
        colors = colors,
        tileMode = TileMode.Clamp
    )
}

@Composable
private fun BannerText(text: String, colors: List<Color>, glow: Color) {
    val style = remember(colors) {
        TextStyle(
            brush = BannerBrush(colors),
            fontWeight = FontWeight.Black,
            fontSize = 27.sp,
            letterSpacing = 7.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(color = glow, offset = Offset.Zero, blurRadius = 18f)
        )
    }
    Text(text = text, style = style)
}

/**
 * A banner's opacity: fades in over [fadeIn], and out again if a fade out is
 * asked for. Both windows are fractions of the celebration timeline.
 */
internal fun bannerAlpha(
    t: Float,
    start: Float,
    fadeIn: Float,
    fadeOutStart: Float,
    fadeOutEnd: Float
): Float {
    if (t < start) return 0f
    val rise = smoothStep((t - start) / max(1e-4f, fadeIn))
    val fall = if (fadeOutStart >= fadeOutEnd) {
        1f
    } else {
        1f - smoothStep((t - fadeOutStart) / max(1e-4f, fadeOutEnd - fadeOutStart))
    }
    return rise * fall
}

/**
 * True when the user has turned animations off system wide, in which case the
 * celebration holds a single quiet moment instead of firing anything.
 */
@Composable
internal fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/*
 * ---------------------------------------------------------------------------
 * DRAWING
 * ---------------------------------------------------------------------------
 */

private val Point = FloatArray(2)
private val Trail = FloatArray(2)

private fun DrawScope.drawCelebration(model: CelebrationModel, time: Float, reduceMotion: Boolean) {
    val fade = celebrationFadeAt(time, model.duration)
    if (fade <= 0f) return

    drawBannerBloom(model, time, fade)

    if (reduceMotion) {
        drawGentleStar(model, time)
        return
    }

    for (index in model.ribbons.indices) drawRibbon(model, index, time, fade)
    for (index in model.shells.indices) {
        drawShellClimb(model, index, time)
        drawBurst(model, index, time, fade)
    }
    for (piece in model.pieces) drawPiece(model, piece, time, fade)
    for (piece in model.glitter) drawPiece(model, piece, time, fade)

    drawScreenFlash(model, time)
}

/** The warm bloom sitting behind the banner, laid down before anything else so
 *  every shell and ribbon reads as brighter than the glow behind them. */
private fun DrawScope.drawBannerBloom(model: CelebrationModel, time: Float, fade: Float) {
    val alpha = smoothStep(time / 0.12f) * (1f - smoothStep((time - 0.34f) / 0.60f)) * fade
    if (alpha <= 0.005f) return
    drawCircle(
        brush = model.bannerBloom,
        radius = model.stage.radius * 1.25f,
        center = Offset(model.stage.centerX, model.stage.centerY - model.stage.radius * 0.14f),
        alpha = alpha
    )
}

/** The frame of white that snaps on over the board when the level lands. */
private fun DrawScope.drawScreenFlash(model: CelebrationModel, time: Float) {
    val alpha = screenFlashAlphaAt(time) * model.tier.flashGain
    if (alpha <= 0.004f) return
    drawRect(color = NeonCore.copy(alpha = 0.62f * alpha))
    // Warm over cool, so the flash itself reads as a colour event rather than a
    // white blink. Two flat halves beat a gradient here: no per frame brush.
    drawRect(
        color = NeonAmber.copy(alpha = 0.22f * alpha),
        size = Size(size.width, size.height * 0.5f)
    )
    drawRect(
        color = NeonMagenta.copy(alpha = 0.16f * alpha),
        topLeft = Offset(0f, size.height * 0.5f),
        size = Size(size.width, size.height * 0.5f)
    )
}

/** The shell still in the air: a hot point with three beads of spent smoke
 *  behind it, which is what sells the climb before the burst opens. */
private fun DrawScope.drawShellClimb(model: CelebrationModel, index: Int, time: Float) {
    val shell = model.shells[index]
    if (!shell.shellAt(time, Point)) return

    val x = Point[0]
    val y = Point[1]
    val rise = smoothStep((time - shell.launchAt) / max(1e-4f, shell.flight * 0.7f))

    for (bead in 3 downTo 1) {
        val behind = shell.shellAt(time - bead * shell.flight * 0.16f, Trail)
        if (!behind) continue
        val a = (1f - bead / 4f) * 0.45f * rise
        drawCircle(
            color = lerp(shell.color, NeonCore, 0.5f).copy(alpha = a),
            radius = model.stage.radius * 0.010f * (4f - bead) / 3f,
            center = Offset(Trail[0], Trail[1])
        )
    }

    drawCircle(
        color = shell.color.copy(alpha = 0.30f * rise),
        radius = model.stage.radius * 0.026f,
        center = Offset(x, y)
    )
    drawCircle(
        color = NeonCore.copy(alpha = 0.95f * rise),
        radius = model.stage.radius * 0.011f,
        center = Offset(x, y)
    )
}

/**
 * A burst, in four parts: the shock ring that comes off it, the white core, the
 * fan of rays, and the grains still falling out of it.
 *
 * All the rays share three paths rather than one path each. Because a shell is a
 * single colour, that loses nothing visually and turns what would be hundreds of
 * strokes per frame into three.
 */
private fun DrawScope.drawBurst(model: CelebrationModel, index: Int, time: Float, fade: Float) {
    val shell = model.shells[index]
    val age = time - shell.burstAt
    if (age < 0f || age > shell.life) return
    val life = shell.life
    val origin = Offset(shell.burstX, shell.burstY)

    // Shock ring: a smoke halo that expands and thins as it goes.
    val ring = age / (life * 0.60f)
    if (ring < 1f) {
        val falloff = (1f - ring) * (1f - ring)
        drawCircle(
            color = lerp(shell.color, NeonCore, 0.45f).copy(alpha = 0.20f * falloff * fade),
            radius = shell.spread * (0.30f + 0.95f * ring),
            center = origin,
            style = Stroke(
                width = shell.spread * 0.085f * (1f - 0.55f * ring),
                cap = StrokeCap.Round
            )
        )
    }

    // The white core of the shell.
    val flash = burstFlashAlphaAt(age, life) * fade
    if (flash > 0.004f) {
        val radius = shell.spread * 0.20f * (0.35f + 0.65f * smoothStep(age / (life * 0.09f)))
        drawCircle(color = NeonCore.copy(alpha = 0.22f * flash), radius = radius * 3.2f, center = origin)
        drawCircle(color = NeonCore.copy(alpha = 0.42f * flash), radius = radius * 1.7f, center = origin)
        drawCircle(color = NeonCore.copy(alpha = 0.95f * flash), radius = radius, center = origin)
    }

    val rayAlpha = burstRayAlphaAt(age, life) * fade
    if (rayAlpha <= 0.004f) return
    val reach = burstRayLengthAt(age, life)

    shell.widePath.rewind()
    shell.midPath.rewind()
    shell.corePath.rewind()
    for (ray in shell.rays) {
        val dx = cos(ray.angle)
        val dy = sin(ray.angle)
        val length = shell.spread * ray.lengthScale * reach
        val x = shell.burstX
        val y = shell.burstY
        shell.widePath.moveTo(x, y)
        shell.widePath.lineTo(x + dx * length, y + dy * length)
        val mid = 0.86f
        shell.midPath.moveTo(x, y)
        shell.midPath.lineTo(x + dx * length * mid, y + dy * length * mid)
        val core = 0.52f
        shell.corePath.moveTo(x, y)
        shell.corePath.lineTo(x + dx * length * core, y + dy * length * core)
    }

    // Three passes of one path each: wide and dim for the glow, then the streak
    // itself, then a white-hot centre line that only reaches halfway down it.
    drawPath(
        path = shell.widePath,
        color = shell.color.copy(alpha = 0.20f * rayAlpha),
        style = Stroke(shell.spread * 0.075f, cap = StrokeCap.Round)
    )
    drawPath(
        path = shell.midPath,
        color = shell.color.copy(alpha = 0.55f * rayAlpha),
        style = Stroke(shell.spread * 0.034f, cap = StrokeCap.Round)
    )
    drawPath(
        path = shell.corePath,
        color = lerp(shell.color, NeonCore, 0.70f).copy(alpha = 0.92f * rayAlpha),
        style = Stroke(shell.spread * 0.013f, cap = StrokeCap.Round)
    )

    // Embers, each one dragging against the air and drooping as it goes.
    val grainAlpha = (1f - smoothStep((age - life * 0.40f) / (life * 0.60f))) * fade
    if (grainAlpha <= 0.004f) return
    val drag = 2.6f
    val gravity = shell.spread * 1.15f
    for (grain in shell.grains) {
        burstGrainOffset(
            age = age,
            life = life,
            speed = shell.speed,
            angle = grain.angle,
            drag = drag,
            gravity = gravity * grain.gravityScale,
            out = Point
        )
        val center = Offset(shell.burstX + Point[0], shell.burstY + Point[1])
        val hot = smoothStep(age / (life * 0.14f))
        val size = shell.spread * 0.020f * grain.size
        drawCircle(
            color = shell.color.copy(alpha = 0.34f * grainAlpha),
            radius = size * 2.2f,
            center = center
        )
        drawCircle(
            color = lerp(shell.color, NeonCore, 1f - hot).copy(alpha = 0.92f * grainAlpha),
            radius = size,
            center = center
        )
    }
}

/**
 * One ribbon: a soft glow pass over the whole sweep, then the sweep itself split
 * into slices that step up in width and brightness, then a white-hot line under
 * the head and a short fan of sparks behind it.
 */
private fun DrawScope.drawRibbon(model: CelebrationModel, index: Int, time: Float, fade: Float) {
    val ribbon = model.ribbons[index]
    val age = time - ribbon.startAt
    val alpha = ribbonAlphaAt(age, ribbon.life) * fade
    if (alpha <= 0.006f) return

    val head = ribbonHeadAt(age, ribbon.life)
    if (head <= 0.001f) return

    ribbon.fullPath.rewind()
    ribbon.pointAt(0f, Point)
    ribbon.fullPath.moveTo(Point[0], Point[1])
    for (step in 1..RibbonSegments) {
        ribbon.pointAt(head * step / RibbonSegments, Point)
        ribbon.fullPath.lineTo(Point[0], Point[1])
    }

    // The soft halo, so the ribbon lights the board around itself.
    drawPath(
        path = ribbon.fullPath,
        color = ribbon.color.copy(alpha = 0.10f * alpha),
        style = Stroke(ribbon.width * 1.95f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = ribbon.fullPath,
        color = ribbon.color.copy(alpha = 0.24f * alpha),
        style = Stroke(ribbon.width * 1.22f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // The sweep itself, stepped so it tapers from its tail to its head.
    val perSlice = RibbonSegments / RibbonSlices
    for (slice in 0 until RibbonSlices) {
        val path = ribbon.slicePaths[slice]
        path.rewind()
        val from = slice * perSlice
        ribbon.pointAt(head * from / RibbonSegments, Point)
        path.moveTo(Point[0], Point[1])
        for (step in from + 1..(from + perSlice)) {
            ribbon.pointAt(head * step / RibbonSegments, Point)
            path.lineTo(Point[0], Point[1])
        }
        val f = (slice + 1f) / RibbonSlices
        drawPath(
            path = path,
            color = lerp(ribbon.color, NeonCore, 0.40f * f).copy(
                alpha = (0.34f + 0.58f * f) * alpha
            ),
            style = Stroke(
                width = ribbon.width * (0.34f + 0.66f * f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // The hot centre line, only under the head, so the ribbon still reads as a
    // ribbon and not as a flat band.
    drawPath(
        path = ribbon.slicePaths[RibbonSlices - 1],
        color = NeonCore.copy(alpha = 0.80f * alpha),
        style = Stroke(ribbon.width * 0.26f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Sparks shed off the head as it travels.
    ribbon.pointAt(head, Point)
    val headX = Point[0]
    val headY = Point[1]
    for (bead in 1..RibbonTailCount) {
        val u = head - bead * RibbonTailStep
        if (u <= 0f) break
        val f = 1f - bead / (RibbonTailCount + 1f)
        ribbon.pointAt(u, Point)
        drawCircle(
            color = lerp(ribbon.color, NeonCore, 0.4f * f).copy(alpha = 0.70f * f * alpha),
            radius = ribbon.width * 0.11f * f,
            center = Offset(Point[0], Point[1])
        )
    }
    drawCircle(
        color = NeonCore.copy(alpha = 0.85f * alpha),
        radius = ribbon.width * 0.16f,
        center = Offset(headX, headY)
    )
}

/**
 * One falling piece. A star or a strip is drawn as a shape that spins about its
 * own centre and shrinks as it lands, with a small ring flash at the moment it
 * touches down; a dot is just a lit point.
 */
private fun DrawScope.drawPiece(
    model: CelebrationModel,
    piece: CelebrationPiece,
    time: Float,
    fade: Float
) {
    val alpha = piece.alphaAt(time) * fade
    if (alpha <= 0.006f) return
    piece.positionAt(time, Point)
    val x = Point[0]
    val y = Point[1]

    if (piece.kind == PIECE_DOT) {
        drawCircle(
            color = piece.color.copy(alpha = 0.28f * alpha),
            radius = piece.size * 2.4f,
            center = Offset(x, y)
        )
        drawCircle(
            color = lerp(piece.color, NeonCore, 0.55f).copy(alpha = 0.92f * alpha),
            radius = piece.size,
            center = Offset(x, y)
        )
        return
    }

    // Past this line a piece has landed: it shrinks out and rings once.
    val landing = ((y - model.stage.groundY) / (model.stage.radius * 0.10f)).coerceIn(0f, 1f)
    if (landing >= 1f) return
    val pop = smoothStep(landing)
    val body = piece.size * (1f - 0.85f * pop)
    val spin = piece.spinAt(time) * (1f - pop * 0.5f)
    val center = Offset(x, y)

    if (landing > 0.02f) {
        drawCircle(
            color = piece.color.copy(alpha = 0.45f * landing * alpha),
            radius = body * (1f + landing * 3.4f),
            center = center,
            style = Stroke(width = body * 0.30f, cap = StrokeCap.Round)
        )
    }

    withTransform({
        translate(x, y)
        rotate(degrees = spin, pivot = Offset.Zero)
        scale(body, body, Offset.Zero)
    }) {
        when (piece.kind) {
            PIECE_STAR -> {
                drawPath(
                    path = model.starPath,
                    color = piece.color.copy(alpha = 0.26f * alpha),
                    style = Stroke(width = 0.22f, join = StrokeJoin.Round)
                )
                drawPath(
                    path = model.starPath,
                    color = piece.color.copy(alpha = 0.94f * alpha)
                )
                drawPath(
                    path = model.starPath,
                    color = NeonCore.copy(alpha = 0.55f * alpha),
                    style = Stroke(width = 0.07f, join = StrokeJoin.Round)
                )
            }

            else -> {
                drawRect(
                    color = piece.color.copy(alpha = 0.30f * alpha),
                    topLeft = Offset(-0.55f, -1f),
                    size = Size(1.1f, 2f)
                )
                drawRect(
                    color = piece.color.copy(alpha = 0.92f * alpha),
                    topLeft = Offset(-0.34f, -1f),
                    size = Size(0.68f, 2f)
                )
                drawRect(
                    color = lerp(piece.color, NeonCore, 0.6f).copy(alpha = 0.80f * alpha),
                    topLeft = Offset(-0.10f, -0.72f),
                    size = Size(0.20f, 1.44f)
                )
            }
        }
    }
}

/** What is left when system animations are off: one soft star blooming and
 *  holding, so the moment still reads as a reward without anything moving fast. */
private fun DrawScope.drawGentleStar(model: CelebrationModel, time: Float) {
    val alpha = (smoothStep(time / 0.20f) * (1f - celebrationFadeAt(time, model.duration))) *
        model.tier.flashGain
    if (alpha <= 0.01f) return
    val stage = model.stage
    val body = stage.radius * 0.20f * (0.86f + 0.14f * easeOutCubic((time / 0.36f).coerceIn(0f, 1f)))
    withTransform({
        translate(stage.centerX, stage.centerY)
        scale(body, body, Offset.Zero)
    }) {
        drawPath(path = model.starPath, color = Gold.copy(alpha = 0.20f * alpha))
        drawPath(path = model.starPath, color = Gold.copy(alpha = 0.92f * alpha))
        drawPath(
            path = model.starPath,
            color = NeonCore.copy(alpha = 0.60f * alpha),
            style = Stroke(width = 0.06f, join = StrokeJoin.Round)
        )
    }
}
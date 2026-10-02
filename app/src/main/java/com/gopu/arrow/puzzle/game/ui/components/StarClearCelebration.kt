package com.gopu.arrow.puzzle.game.ui.components

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.theme.FlameOrange
import com.gopu.arrow.puzzle.game.ui.theme.FlameYellow
import com.gopu.arrow.puzzle.game.ui.theme.Gold
import com.gopu.arrow.puzzle.game.ui.theme.NeonAmber
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.NeonGreen
import com.gopu.arrow.puzzle.game.ui.theme.NeonMagenta
import com.gopu.arrow.puzzle.game.ui.theme.NeonText
import com.gopu.arrow.puzzle.game.ui.theme.StageVoid
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * ---------------------------------------------------------------------------
 * LEVEL CLEAR CELEBRATION
 * ---------------------------------------------------------------------------
 *
 * A pure presentation layer. It reads the star score the level already earned
 * and plays the matching celebration on a canvas of its own, so gameplay,
 * geometry, scoring and layout are all left exactly as they were.
 *
 * Everything on screen is built from the real gameplay pieces: the arrows are
 * the same [ArrowShape] the maze draws, scaled down and carried along a scripted
 * path, and the colours come from the same neon palette. A celebrated arrow is
 * a rigid body: it is only ever translated and rotated, never stretched, and its
 * heading always follows the path it is actually travelling, so an arrow that
 * flies a curve points along that curve.
 *
 * Compactness is enforced numerically rather than by eye. Every glow layer is a
 * fraction of the arrow's own tube width, the ribbons are capped at a quarter
 * wider than that tube, the particles are a few pixels across, and the whole
 * effect is laid out inside a circle inscribed in the box it is handed.
 */

private const val DegreesToRadians = (PI / 180.0).toFloat()
private const val RadiansToDegrees = (180.0 / PI).toFloat()

/** Sampling step used to derive an arrow's heading from its own path. */
private const val HeadingEpsilon = 1f / 240f

/** How much of the way to the new heading an arrow turns each frame. Damping the
 *  turn is what takes the last shimmer out of the sampling and leaves the motion
 *  reading as one smooth sweep. */
private const val HeadingEase = 0.38f

/** Body samples. The body is redrawn from scratch every frame, so the count is a
 *  straight trade between smoothness and cost; 26 reads as a continuous curve
 *  even at the fastest orbit speed. */
private const val BodySegments = 26

/** Extra width of the flex body over the arrow's own tube, as a fraction of it.
 *  The body is meant to be the arrow's tube, so this is a hairline rim only and
 *  the arrow itself always reads as the sharpest thing on screen. */
private const val BodyWidthFactor = 1.06f

private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun easeOutCubic(t: Float): Float {
    val x = 1f - t.coerceIn(0f, 1f)
    return 1f - x * x * x
}

/** Overshoots a little and settles, so a growing star lands with a snap. */
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

private val StarFill = Brush.radialGradient(
    0.00f to NeonCore,
    0.26f to FlameYellow,
    0.60f to Gold,
    1.00f to FlameOrange,
    center = Offset.Zero,
    radius = 1.18f
)

/**
 * How much celebration a finished level earns. The tier is read straight from
 * the star score the level already produced, so the spectacle is a direct read of
 * how well the player did and is never rolled at random.
 */
private enum class ClearTier(
    val arrowCount: Int,
    val archetypes: IntArray,
    val unitScale: Float,
    val orbitScale: Float,
    val entryScale: Float,
    val starScale: Float,
    val bodyScale: Float,
    val sparkBursts: Int,
    val sparksPerBurst: Int,
    val confettiCount: Int,
    val spinRange: ClosedFloatingPointRange<Float>,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val hopAmp: Float,
    val stagger: Float,
    val flight: Float,
    val pulses: FloatArray,
    val pulseGain: Float,
    val showPerfect: Boolean,
    val perfectAt: Float,
    val clearAt: Float,
    val durationMillis: Int
) {
    /** "I completed it." Clean and elegant: three arrows, a short flexing body,
     *  one soft breath of light. */
    SIMPLE(
        arrowCount = 3,
        archetypes = intArrayOf(1, 4, 7),
        unitScale = 0.235f,
        orbitScale = 0.55f,
        entryScale = 0.78f,
        starScale = 0.150f,
        bodyScale = 0.24f,
        sparkBursts = 1,
        sparksPerBurst = 8,
        confettiCount = 0,
        spinRange = 0.46f..0.66f,
        wobbleAmp = 0.10f,
        wobbleFreq = 1.25f,
        hopAmp = 0.05f,
        stagger = 0.08f,
        flight = 0.55f,
        pulses = floatArrayOf(0.46f),
        pulseGain = 0.55f,
        showPerfect = false,
        perfectAt = 1f,
        clearAt = 0.44f,
        durationMillis = 1700
    ),

    /** "Nice performance." Five arrows on real orbits with long curving bodies. */
    GOOD(
        arrowCount = 5,
        archetypes = intArrayOf(1, 2, 4, 6, 0),
        unitScale = 0.255f,
        orbitScale = 0.60f,
        entryScale = 0.80f,
        starScale = 0.190f,
        bodyScale = 0.42f,
        sparkBursts = 1,
        sparksPerBurst = 14,
        confettiCount = 8,
        spinRange = 0.74f..1.00f,
        wobbleAmp = 0.10f,
        wobbleFreq = 1.45f,
        hopAmp = 0.045f,
        stagger = 0.06f,
        flight = 0.68f,
        pulses = floatArrayOf(0.58f, 0.98f),
        pulseGain = 0.85f,
        showPerfect = false,
        perfectAt = 1f,
        clearAt = 0.48f,
        durationMillis = 2400
    ),

    /** "Perfect! That was special." Six arrows, mixed spin directions, the longest
     *  bodies and the full burst. */
    PERFECT(
        arrowCount = 6,
        archetypes = intArrayOf(1, 2, 3, 5, 6, 4),
        unitScale = 0.265f,
        orbitScale = 0.62f,
        entryScale = 0.80f,
        starScale = 0.215f,
        bodyScale = 0.56f,
        sparkBursts = 3,
        sparksPerBurst = 10,
        confettiCount = 16,
        spinRange = 0.84f..1.16f,
        wobbleAmp = 0.12f,
        wobbleFreq = 1.70f,
        hopAmp = 0.07f,
        stagger = 0.045f,
        flight = 0.78f,
        pulses = floatArrayOf(0.96f),
        pulseGain = 1f,
        showPerfect = true,
        perfectAt = 0.32f,
        clearAt = 0.64f,
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

/** The compact stage the celebration is laid out in. */
private class CelebrationStage(
    val width: Float,
    val height: Float,
    val tier: ClearTier
) {
    val centerX = width * 0.5f
    val centerY = height * 0.5f
    val radius = min(width, height) * 0.5f
    val arrowUnit = radius * tier.unitScale
    val stroke = arrowUnit * 0.088f
    val orbitRadius = radius * tier.orbitScale
    val entryRadius = radius * tier.entryScale
    val starRadius = radius * tier.starScale
    /** How long a fully grown flex body is, and the arc-length gap between its
     *  samples. Sampling by distance, not by frame, is what keeps the body the
     *  same length no matter how the frame rate moves. */
    val bodyLength = radius * tier.bodyScale
    val bodySpacing = bodyLength / BodySegments
}

/**
 * One celebration arrow: a real [ArrowShape] plus the scripted path it flies.
 *
 * The path is a curved approach that hands over to a wobbling orbit. The two are
 * blended with a smoothstep, whose derivative vanishes at both ends, so the
 * arrow leaves the entry on its curve velocity and arrives already travelling on
 * the orbit tangent. There is no visible seam and nothing snaps.
 */
private class CelebratingArrow(
    val shape: ArrowShape,
    val color: Color,
    val startDelay: Float,
    val flight: Float,
    val entryAngle: Float,
    val entryRadius: Float,
    val orbitAngle: Float,
    val orbitRadius: Float,
    val spin: Float,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val wobblePhase: Float,
    val hopAmp: Float,
    val hopFreq: Float,
    val hopPhase: Float,
    val curl: Float,
    val scale: Float,
    val glow: Float,
    val rotWobble: Float,
    val fadeIn: Float
) {

    /** Where the arrow's pivot sits at time [t], in stage pixels. */
    fun position(t: Float, stage: CelebrationStage): Offset {
        val angle = orbitAngle + spin * t
        val radialX = cos(angle)
        val radialY = sin(angle)
        val radius = stage.orbitRadius * orbitRadius *
            (1f + wobbleAmp * sin(wobbleFreq * t + wobblePhase))

        var targetX = stage.centerX + radialX * radius
        var targetY = stage.centerY + radialY * radius

        if (hopAmp > 0f) {
            // A gentle bounce across the orbit, so the arrow reads as alive
            // rather than as a sprite on a rail.
            val hop = stage.orbitRadius * hopAmp * orbitRadius *
                sin(hopFreq * t + hopPhase)
            targetX += -radialY * hop
            targetY += radialX * hop
        }

        if (t >= flight) return Offset(targetX, targetY)

        val startX = stage.centerX + cos(entryAngle) * entryRadius
        val startY = stage.centerY + sin(entryAngle) * entryRadius
        val dx = targetX - startX
        val dy = targetY - startY
        val length = sqrt(dx * dx + dy * dy)
        if (length < 0.001f) return Offset(targetX, targetY)

        val u = t / flight
        val inverse = 1f - u
        val pull = length * curl
        val controlX = startX + dx * 0.62f - dy / length * pull
        val controlY = startY + dy * 0.62f + dx / length * pull

        val a = inverse * inverse
        val b = 2f * inverse * u
        val c = u * u
        val curveX = a * startX + b * controlX + c * targetX
        val curveY = a * startY + b * controlY + c * targetY

        val blend = smoothStep(u)
        return Offset(
            x = curveX + (targetX - curveX) * blend,
            y = curveY + (targetY - curveY) * blend
        )
    }
}

/** Where one celebration arrow ended up this frame, solved before anything draws. */
private class ArrowFrame {
    var x = 0f
    var y = 0f
    var heading = 0f
    var scale = 1f
    var alpha = 0f
    var glow = 0f
    var tailX = 0f
    var tailY = 0f
    var settled = false
}

/**
 * The flexible body trailing one arrow, and the ribbon of light inside it.
 *
 * This is what makes the arrows bend. Instead of a rigid sprite sliding along a
 * line, the arrow's tail point is recorded every frame and the stretch of path it
 * has just swept is rebuilt into a smooth tube. The head is still the game's own
 * [ArrowShape] head; the body simply keeps flowing out of its tail, following
 * the real curve the arrow flew, which is why a bent arrow's ribbon bends with it
 * instead of cutting across the corner.
 *
 * The tube is painted with exactly the recipe [ArrowShape] uses for its own
 * shaft, at the same width, so the body and the arrow are visibly one object.
 * Three overlapping slices of the centre line fade the far end out, and the
 * stored points are re-sampled by arc length every frame, so the taper stays put
 * whatever the frame rate does.
 */
private class FlexBody {
    private val capacity = 72
    private val historyX = FloatArray(capacity)
    private val historyY = FloatArray(capacity)
    private val pointX = FloatArray(capacity)
    private val pointY = FloatArray(capacity)
    private val cumulative = FloatArray(capacity)
    private val sampleX = FloatArray(BodySegments + 1)
    private val sampleY = FloatArray(BodySegments + 1)

    private var head = 0
    private var size = 0
    private var primed = false
    private var lastX = 0f
    private var lastY = 0f

    /** Fade in from the tail, on the widest layer only. */
    val tubeFar = Path()
    val tubeMid = Path()
    val tubeNear = Path()
    val full = Path()
    val core = Path()

    fun reset() {
        head = 0
        size = 0
        primed = false
        tubeFar.rewind()
        tubeMid.rewind()
        tubeNear.rewind()
        full.rewind()
        core.rewind()
    }

    fun push(x: Float, y: Float) {
        if (primed) {
            val dx = x - lastX
            val dy = y - lastY
            // Sub pixel drift would only add noise to the samples.
            if (dx * dx + dy * dy < 1f) return
        }
        head = (head - 1 + capacity) % capacity
        historyX[head] = x
        historyY[head] = y
        if (size < capacity) size++
        lastX = x
        lastY = y
        primed = true
    }

    fun build(spacing: Float) {
        if (size < 3 || spacing <= 0f) {
            reset()
            return
        }

        for (index in 0 until size) {
            val slot = (head - index + capacity) % capacity
            pointX[index] = historyX[slot]
            pointY[index] = historyY[slot]
        }

        cumulative[0] = 0f
        for (index in 1 until size) {
            val dx = pointX[index] - pointX[index - 1]
            val dy = pointY[index] - pointY[index - 1]
            cumulative[index] = cumulative[index - 1] + sqrt(dx * dx + dy * dy)
        }

        val usable = min(cumulative[size - 1], spacing * BodySegments)
        var cursor = size - 1
        for (step in 0..BodySegments) {
            val distance = max(0f, usable - step * spacing)
            while (cursor > 0 && cumulative[cursor] > distance) cursor--
            if (cursor >= size - 1) {
                sampleX[step] = pointX[size - 1]
                sampleY[step] = pointY[size - 1]
            } else {
                val span = cumulative[cursor + 1] - cumulative[cursor]
                val t = if (span < 0.0001f) 0f else (distance - cumulative[cursor]) / span
                sampleX[step] = pointX[cursor] + (pointX[cursor + 1] - pointX[cursor]) * t
                sampleY[step] = pointY[cursor] + (pointY[cursor + 1] - pointY[cursor]) * t
            }
        }

        // Sample BodySegments is the newest one, welded to the arrow's tail. Each
        // slice starts later and is brighter, so the tube reads as fading out
        // behind the arrow rather than stopping dead.
        trace(full, 0)
        trace(tubeFar, 0)
        trace(tubeMid, BodySegments * 22 / 100)
        trace(tubeNear, BodySegments * 50 / 100)
        trace(core, BodySegments * 62 / 100)
    }

    private fun trace(path: Path, from: Int) {
        path.rewind()
        if (from >= BodySegments) return
        path.moveTo(sampleX[from], sampleY[from])
        for (index in from + 1..BodySegments) {
            path.lineTo(sampleX[index], sampleY[index])
        }
    }
}

/**
 * One small light. Its motion is closed form, so a particle costs a couple of
 * multiplies per frame, never accumulates error, and can be scrubbed backwards
 * without drifting.
 */
private class CelebrationSpark(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val drag: Float,
    val gravity: Float,
    val delay: Float,
    val life: Float,
    val radius: Float,
    val streak: Float,
    val color: Color
) {
    /** Zero while the spark has not started, zero again once it has burnt out. */
    fun alphaAt(time: Float): Float {
        val age = time - delay
        if (age < 0f || age > life) return 0f
        val rise = smoothStep(age / max(1e-4f, life * 0.10f))
        val fall = 1f - age / life
        return rise * fall * fall
    }

    fun positionAt(time: Float, out: FloatArray) {
        val age = (time - delay).coerceIn(0f, life)
        val travel = if (drag < 0.0001f) age else (1f - exp(-drag * age)) / drag
        out[0] = x + vx * travel
        out[1] = y + vy * travel + 0.5f * gravity * age * age
    }
}

/** Everything the celebration draws, built once per run. */
private class CelebrationModel(
    val stage: CelebrationStage,
    val tier: ClearTier,
    val arrows: List<CelebratingArrow>,
    val frames: List<ArrowFrame>,
    val bodies: List<FlexBody>,
    val sparks: List<CelebrationSpark>,
    val starPath: Path,
    val duration: Float
) {
    companion object {

        fun build(width: Float, height: Float, stars: Int): CelebrationModel {
            val tier = ClearTier.of(stars)
            val stage = CelebrationStage(width, height, tier)
            // Seeded per run, so the layout is settled rather than jittery and
            // nothing random is generated inside a frame.
            val random = Random(stars * 7919 + width.toInt() * 31 + height.toInt())
            val shapes = celebrationShapes(stage.arrowUnit)
            val arrows = ArrayList<CelebratingArrow>(tier.arrowCount)

            for (index in 0 until tier.arrowCount) {
                val archetype = tier.archetypes[index % tier.archetypes.size]
                val shape = shapes[archetype]
                val pivot = shape.pivot

                // How far the arrow reaches behind its pivot. Used to keep the
                // entry point inside the stage instead of clipping the tail.
                val rear = shape.spine.maxOf { (it - pivot).getDistance() } +
                    shape.strokeWidth * 0.5f

                // Three stars send a few arrows the other way round, which is
                // most of what makes the premium tier feel busy.
                val spinSign = if (tier == ClearTier.PERFECT && index % 3 == 0) -1f else 1f
                val spin = spinSign *
                    random.between(tier.spinRange.start, tier.spinRange.endInclusive)
                val orbitAngle = (-PI / 2.0 +
                    index * (2.0 * PI / tier.arrowCount) +
                    random.nextDouble(-0.16, 0.16)).toFloat()

                arrows += CelebratingArrow(
                    shape = shape,
                    color = NeonPalette[(index * 3 + archetype) % NeonPalette.size],
                    startDelay = index * tier.stagger,
                    flight = tier.flight,
                    entryAngle = orbitAngle + spin * tier.flight + PI.toFloat(),
                    // At the entry the arrow points inward, so its whole length lies
                    // along the radius. Keep the tail inside the stage: clipping a
                    // ribbon against the screen edge is the one thing that would
                    // give the effect away.
                    entryRadius = min(
                        stage.entryRadius,
                        stage.radius * 0.995f - rear * 0.94f
                    ),
                    orbitAngle = orbitAngle,
                    orbitRadius = random.between(0.94f, 1.08f),
                    spin = spin,
                    wobbleAmp = tier.wobbleAmp * random.between(0.85f, 1.15f),
                    wobbleFreq = tier.wobbleFreq * random.between(0.9f, 1.1f),
                    wobblePhase = random.between(0f, 2f * PI.toFloat()),
                    hopAmp = tier.hopAmp * random.between(0.7f, 1.3f),
                    hopFreq = 1.9f * random.between(0.85f, 1.2f),
                    hopPhase = random.between(0f, 2f * PI.toFloat()),
                    curl = random.between(-0.26f, 0.26f),
                    scale = random.between(0.94f, 1.06f),
                    glow = random.between(0.18f, 0.42f),
                    rotWobble = if (tier == ClearTier.SIMPLE) {
                        0f
                    } else {
                        random.between(1.2f, 3.4f)
                    },
                    // A longer fade-in lets the arrows arrive from just off the edge
                    // of the stage without ever popping into view.
                    fadeIn = 0.18f
                )
            }

            return CelebrationModel(
                stage = stage,
                tier = tier,
                arrows = arrows,
                frames = List(arrows.size) { ArrowFrame() },
                bodies = List(arrows.size) { FlexBody() },
                sparks = buildSparks(tier, stage, random),
                starPath = unitStarPath(),
                duration = tier.durationMillis / 1000f
            )
        }

        private fun buildSparks(
            tier: ClearTier,
            stage: CelebrationStage,
            random: Random
        ): List<CelebrationSpark> {
            val sparks = ArrayList<CelebrationSpark>(
                tier.sparkBursts * tier.sparksPerBurst + tier.confettiCount
            )

            for (burst in 0 until tier.sparkBursts) {
                val delay = 0.30f + burst * 0.24f
                val originX = stage.centerX + random.between(-0.10f, 0.10f) * stage.radius
                val originY = stage.centerY + random.between(-0.10f, 0.10f) * stage.radius
                val speed = stage.radius * random.between(0.42f, 0.66f)
                for (index in 0 until tier.sparksPerBurst) {
                    val angle = (index * 2.0 * PI / tier.sparksPerBurst +
                        random.nextDouble(-0.22, 0.22)).toFloat()
                    val push = speed * random.between(0.7f, 1.15f)
                    sparks += CelebrationSpark(
                        x = originX,
                        y = originY,
                        vx = cos(angle) * push,
                        vy = sin(angle) * push,
                        drag = 2.4f,
                        gravity = stage.radius * 0.55f,
                        delay = delay + random.between(0f, 0.05f),
                        life = random.between(0.55f, 0.85f),
                        radius = stage.radius * random.between(0.006f, 0.013f),
                        streak = 0f,
                        color = NeonPalette[(index + burst * 2) % NeonPalette.size]
                    )
                }
            }

            for (index in 0 until tier.confettiCount) {
                sparks += CelebrationSpark(
                    x = stage.centerX + random.between(-0.62f, 0.62f) * stage.radius,
                    y = stage.centerY - stage.radius * random.between(0.05f, 0.45f),
                    vx = random.between(-0.10f, 0.10f) * stage.radius,
                    vy = random.between(0.10f, 0.30f) * stage.radius,
                    drag = 0.55f,
                    gravity = stage.radius * 0.85f,
                    delay = 0.34f + index * 0.022f,
                    life = random.between(0.75f, 1.05f),
                    radius = stage.radius * random.between(0.008f, 0.014f),
                    streak = stage.radius * random.between(0.035f, 0.06f),
                    color = NeonPalette[(index * 2 + 1) % NeonPalette.size]
                )
            }

            return sparks
        }

        /**
         * The arrow bodies the celebration reuses. They are built by the very same
         * [ArrowShape] the maze uses, at the celebration's own cell size, so a
         * celebrated arrow is the game's arrow: same tube, same head, same neon
         * layering, only smaller and on a different path.
         */
        private fun celebrationShapes(unit: Float): List<ArrowShape> {
            val list = ArrayList<ArrowShape>(8)
            fun add(vararg cells: BoardPosition, direction: Direction) {
                list += ArrowShape(ArrowTile(cells.toList(), direction), unit, unit, 0f)
            }

            add(BoardPosition(0, 0), direction = Direction.RIGHT)
            add(BoardPosition(0, 0), BoardPosition(0, 1), direction = Direction.RIGHT)
            add(BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2), direction = Direction.RIGHT)
            add(BoardPosition(0, 0), BoardPosition(1, 0), BoardPosition(1, 1), direction = Direction.RIGHT)
            add(BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(1, 1), direction = Direction.DOWN)
            add(
                BoardPosition(0, 0), BoardPosition(1, 0), BoardPosition(1, 1), BoardPosition(2, 1),
                direction = Direction.DOWN
            )
            add(
                BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(1, 1), BoardPosition(1, 2),
                direction = Direction.DOWN
            )
            add(BoardPosition(0, 0), BoardPosition(1, 0), direction = Direction.DOWN)
            return list
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
 * One star earns a clean, quiet moment; two stars add orbits, ribbons and a
 * confetti burst; three stars add the full premium treatment, including a brief
 * PERFECT! before the LEVEL CLEAR! banner.
 *
 * The composable owns the only clock in the effect and runs the timeline and the
 * closing fade itself, so leaving the screen cancels all of it in one place, no
 * animation can outlive it, and the result card can be handed a plain "I am done"
 * callback with no transition state of its own.
 */
@Composable
fun StarClearCelebration(
    stars: Int,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val tier = ClearTier.of(stars)
    val clock = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(stars) {
        clock.snapTo(0f)
        clock.animateTo(1f, tween(tier.durationMillis, easing = LinearEasing))
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
                drawCelebration(model, clock.value.coerceIn(0f, 1f) * model.duration)
            }
            CelebrationBanner(tier = tier, radiusPx = model.stage.radius, clock = clock)
        }
    }
}

/** PERFECT! on three stars, then LEVEL CLEAR! on every tier. */
@Composable
private fun CelebrationBanner(
    tier: ClearTier,
    radiusPx: Float,
    clock: Animatable<Float, *>
) {
    val style = remember {
        TextStyle(
            color = NeonText,
            fontWeight = FontWeight.Black,
            fontSize = 27.sp,
            letterSpacing = 7.sp,
            textAlign = TextAlign.Center
        )
    }
    // Sits just outside the orbit ring, so the banner never lands on an arrow or
    // on the HUD no matter how the arrows are placed.
    val offset = with(LocalDensity.current) { (radiusPx * 0.86f).toDp() }

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
                            start = tier.perfectAt,
                            fadeIn = 0.05f,
                            fadeOutStart = tier.clearAt - 0.07f,
                            fadeOutEnd = tier.clearAt
                        )
                        val rise = ((t - tier.perfectAt) / 0.14f).coerceIn(0f, 1f)
                        val s = 0.86f + 0.14f * easeOutBack(rise)
                        scaleX = s
                        scaleY = s
                    }
            ) {
                BannerText("PERFECT!", style, NeonCyan)
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
                        start = tier.clearAt,
                        fadeIn = 0.06f,
                        fadeOutStart = 2f,
                        fadeOutEnd = 2.1f
                    )
                    val rise = ((t - tier.clearAt) / 0.16f).coerceIn(0f, 1f)
                    val s = 0.86f + 0.14f * easeOutBack(rise)
                    scaleX = s
                    scaleY = s
                }
        ) {
            BannerText("LEVEL CLEAR!", style, if (tier.showPerfect) NeonMagenta else NeonGreen)
        }
    }
}

@Composable
private fun BannerText(text: String, style: TextStyle, glow: Color) {
    val glowed = remember(style, glow) {
        style.copy(shadow = Shadow(color = glow, offset = Offset.Zero, blurRadius = 15f))
    }
    Text(text = text, style = glowed)
}

/**
 * A banner's opacity: fades in over [fadeIn], and out again if a fade out is
 * asked for. Both windows are fractions of the celebration timeline.
 */
private fun bannerAlpha(
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

/*
 * ---------------------------------------------------------------------------
 * DRAWING
 * ---------------------------------------------------------------------------
 */

private val CelebrationScratch = FloatArray(2)

private fun DrawScope.drawCelebration(model: CelebrationModel, time: Float) {
    val stage = model.stage
    val tier = model.tier

    solveArrowFrames(model, time)

    val pulse = strongestPulse(tier, time) * tier.pulseGain
    val starScale = starScaleAt(tier, time)
    val starAlpha = smoothStep(time / 0.16f)

    // The star's halo sits furthest back, so the arrows and their ribbons always
    // read as the brightest things on screen.
    if (starAlpha > 0.01f) drawStarGlow(model, starScale, pulse, starAlpha)
    drawStarRings(model, time, starScale, starAlpha)

    for (index in model.arrows.indices) drawFlexBody(model, index)
    if (starAlpha > 0.01f) drawStarBody(model, starScale, pulse, starAlpha)
    for (index in model.arrows.indices) drawArrow(model, index)

    drawSparks(model, time)
}

/**
 * Works out where every arrow is and which way it faces before a single thing is
 * painted, so the flexible bodies behind them can be laid down first.
 *
 * The heading is the direction of travel along the arrow's own path, sampled a
 * fraction of a frame apart, so a curving arrow points along its curve rather
 * than snapping between grid directions. It is then eased towards that target by a
 * fixed fraction each frame, which removes the last of the sampling jitter and
 * leaves the turn reading as one continuous sweep. The very first solved frame
 * snaps instead, so no arrow ever visibly swings in from a default angle.
 */
private fun solveArrowFrames(model: CelebrationModel, time: Float) {
    val stage = model.stage
    for (index in model.arrows.indices) {
        val arrow = model.arrows[index]
        val frame = model.frames[index]
        val age = time - arrow.startDelay

        if (age < 0f) {
            frame.alpha = 0f
            frame.settled = false
            model.bodies[index].reset()
            continue
        }

        val here = arrow.position(age, stage)
        val ahead = arrow.position(age + HeadingEpsilon, stage)
        val target = (
            atan2(ahead.y - here.y, ahead.x - here.x) * RadiansToDegrees +
                arrow.rotWobble * sin(2.4f * age + arrow.wobblePhase)
            ).toFloat()

        val heading = if (frame.settled) {
            frame.heading + shortestTurn(frame.heading, target) * HeadingEase
        } else {
            frame.settled = true
            target
        }

        val radians = heading * DegreesToRadians
        val cos = cos(radians)
        val sin = sin(radians)
        val alpha = smoothStep(age / max(1e-4f, arrow.fadeIn))
        val pivot = arrow.shape.pivot
        val tail = arrow.shape.spine.first()
        val dx = (tail.x - pivot.x) * arrow.scale
        val dy = (tail.y - pivot.y) * arrow.scale

        frame.x = here.x
        frame.y = here.y
        frame.heading = heading
        frame.scale = arrow.scale
        frame.alpha = alpha
        frame.glow = arrow.glow * (0.35f + 0.65f * alpha)
        frame.tailX = here.x + dx * cos - dy * sin
        frame.tailY = here.y + dx * sin + dy * cos
    }
}

/** Signed shortest way round from [from] to [to] in degrees, in (-180, 180]. */
private fun shortestTurn(from: Float, to: Float): Float {
    var delta = (to - from) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

/**
 * The flexible body, painted with the same five layers [ArrowShape] uses for its
 * own shaft and at the same tube width, so the body and the arrow read as one
 * continuous neon object rather than an arrow with something stuck on its tail.
 */
private fun DrawScope.drawFlexBody(model: CelebrationModel, index: Int) {
    val frame = model.frames[index]
    if (frame.alpha <= 0.02f) return

    val body = model.bodies[index]
    body.push(frame.tailX, frame.tailY)
    body.build(model.stage.bodySpacing)

    val color = model.arrows[index].color
    val alpha = frame.alpha
    val unit = model.stage.arrowUnit
    val tube = model.stage.stroke * BodyWidthFactor
    val stroke = Stroke(tube, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Dark separation keyline, same job as the arrow's own.
    drawPath(
        path = body.full,
        color = StageVoid.copy(alpha = 0.92f * alpha),
        style = Stroke(
            width = tube + unit * 0.075f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    // Soft ambient glow and tight rim: a fraction wider than the tube, no halo.
    drawPath(
        path = body.full,
        color = lerp(StageVoid, NeonCore, 0.55f).copy(alpha = 0.07f * alpha),
        style = Stroke(
            width = tube * 1.30f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    drawPath(
        path = body.full,
        color = lerp(StageVoid, NeonCore, 0.55f).copy(alpha = 0.26f * alpha),
        style = Stroke(
            width = tube * 1.08f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    // The tube itself, in three overlapping slices so the far end dissolves.
    drawPath(path = body.tubeFar, color = color.copy(alpha = 0.38f * alpha), style = stroke)
    drawPath(path = body.tubeMid, color = color.copy(alpha = 0.70f * alpha), style = stroke)
    drawPath(path = body.tubeNear, color = color.copy(alpha = 0.95f * alpha), style = stroke)
    // White-hot core, matching the arrow's own centre line.
    drawPath(
        path = body.core,
        color = lerp(color, NeonCore, 0.62f).copy(alpha = 0.90f * alpha),
        style = Stroke(
            width = tube * 0.34f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawArrow(model: CelebrationModel, index: Int) {
    val frame = model.frames[index]
    if (frame.alpha <= 0.01f) return
    val arrow = model.arrows[index]
    val shape = arrow.shape
    val pivot = shape.pivot

    withTransform({
        // Turn about the arrow's own pivot, then carry that pivot to where the
        // path says it is. Rigid: nothing stretches or skews, and the head keeps
        // pointing the way the arrow is actually travelling.
        rotate(degrees = frame.heading, pivot = pivot)
        translate(frame.x - pivot.x, frame.y - pivot.y)
    }) {
        shape.draw(
            scope = this,
            color = arrow.color,
            glow = frame.glow,
            translation = Offset.Zero,
            scale = frame.scale,
            alpha = frame.alpha
        )
    }
}

private fun starScaleAt(tier: ClearTier, time: Float): Float =
    if (tier == ClearTier.PERFECT) {
        // Small to large, with a single clean overshoot on the way up.
        0.30f + 0.70f * easeOutBack(((time - 0.24f) / 0.58f).coerceIn(0f, 1f))
    } else {
        0.80f + 0.20f * easeOutCubic((time / 0.30f).coerceIn(0f, 1f))
    }

/** How strongly the star is lit at this instant, from whichever pulse is loudest. */
private fun strongestPulse(tier: ClearTier, time: Float): Float {
    var peak = 0f
    for (start in tier.pulses) {
        if (time < start) continue
        val q = ((time - start) / 0.46f).coerceIn(0f, 1f)
        val value = (1f - q) * (1f - q)
        if (value > peak) peak = value
    }
    return peak
}

/**
 * The star's halo: two tight layers, both barely wider than the star itself, so a
 * pulse reads as the star lighting up rather than as a bloom spilling over the
 * board.
 */
private fun DrawScope.drawStarGlow(
    model: CelebrationModel,
    scale: Float,
    pulse: Float,
    alpha: Float
) {
    val stage = model.stage
    val radius = stage.starRadius * scale
    val lift = 0.34f + 0.66f * pulse
    val tint = lerp(NeonAmber, NeonCore, 0.34f)

    withTransform({
        translate(stage.centerX, stage.centerY)
        scale(radius * 1.26f, radius * 1.26f)
    }) {
        drawPath(path = model.starPath, color = tint.copy(alpha = 0.08f * lift * alpha))
    }
    withTransform({
        translate(stage.centerX, stage.centerY)
        scale(radius * 1.12f, radius * 1.12f)
    }) {
        drawPath(path = model.starPath, color = tint.copy(alpha = 0.13f * lift * alpha))
    }
}

/** The ring that leaves the star on each pulse. Stops at a third wider than the
 *  star, so it never becomes a screen wide flash. */
private fun DrawScope.drawStarRings(
    model: CelebrationModel,
    time: Float,
    scale: Float,
    alpha: Float
) {
    val stage = model.stage
    val radius = stage.starRadius * scale
    for (start in model.tier.pulses) {
        if (time < start) continue
        val q = ((time - start) / 0.52f).coerceIn(0f, 1f)
        if (q >= 1f) continue
        val tint = lerp(NeonAmber, NeonCore, 0.30f + 0.5f * (1f - q))
        drawCircle(
            color = tint.copy(alpha = 0.20f * (1f - q) * alpha),
            radius = radius * (1f + 0.34f * q),
            style = Stroke(
                width = radius * 0.09f * (1f - 0.6f * q),
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawStarBody(
    model: CelebrationModel,
    scale: Float,
    pulse: Float,
    alpha: Float
) {
    val stage = model.stage
    val radius = stage.starRadius * scale

    withTransform({
        translate(stage.centerX, stage.centerY)
        scale(radius, radius)
    }) {
        drawPath(path = model.starPath, brush = StarFill, alpha = alpha)
        drawPath(
            path = model.starPath,
            color = NeonCore.copy(alpha = (0.34f + 0.46f * pulse) * alpha),
            style = Stroke(width = 0.05f, join = StrokeJoin.Round)
        )
        // A brief white flash through the middle of a pulse, still inside the
        // star's own silhouette.
        if (pulse > 0.01f) {
            withTransform({ scale(0.52f, 0.52f) }) {
                drawPath(
                    path = model.starPath,
                    color = NeonCore.copy(alpha = 0.24f * pulse * alpha)
                )
            }
        }
    }
}

private fun DrawScope.drawSparks(model: CelebrationModel, time: Float) {
    for (spark in model.sparks) {
        val alpha = spark.alphaAt(time)
        if (alpha <= 0.02f) continue
        spark.positionAt(time, CelebrationScratch)
        val x = CelebrationScratch[0]
        val y = CelebrationScratch[1]

        if (spark.streak > 0f) {
            // Confetti: a short streak along its own direction of travel, so the
            // strip tumbles with the fall instead of hanging still.
            spark.positionAt(time - 0.05f, CelebrationScratch)
            val tx = x - CelebrationScratch[0]
            val ty = y - CelebrationScratch[1]
            val length = sqrt(tx * tx + ty * ty)
            if (length > 0.5f) {
                val scale = spark.streak / length
                drawLine(
                    color = spark.color.copy(alpha = 0.70f * alpha),
                    start = Offset(x - tx * scale, y - ty * scale),
                    end = Offset(x, y),
                    strokeWidth = spark.radius * 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lerp(spark.color, NeonCore, 0.55f).copy(alpha = 0.85f * alpha),
                    start = Offset(x - tx * scale * 0.45f, y - ty * scale * 0.45f),
                    end = Offset(x, y),
                    strokeWidth = spark.radius * 0.9f,
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(
                    color = spark.color.copy(alpha = 0.70f * alpha),
                    radius = spark.radius,
                    center = Offset(x, y)
                )
            }
        } else {
            drawCircle(
                color = spark.color.copy(alpha = 0.50f * alpha),
                radius = spark.radius * 1.9f,
                center = Offset(x, y)
            )
            drawCircle(
                color = lerp(spark.color, NeonCore, 0.72f).copy(alpha = 0.95f * alpha),
                radius = spark.radius,
                center = Offset(x, y)
            )
        }
    }
}

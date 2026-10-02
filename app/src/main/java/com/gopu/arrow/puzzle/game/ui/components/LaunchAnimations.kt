package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.StageVoid
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/**
 * How a tapped arrow leaves the board: liquid running through a pipe.
 *
 * The arrow's own bent path is the pipe, and nothing about that path is ever
 * changed. Nothing is bent, flexed, scaled, squashed or re-pathed, and the arrow
 * is never slid across the board as one rigid piece: there is no translation at
 * all until the body has already collapsed to a line.
 *
 * Instead a flow front walks the arrow's real flattened spine from tail to tip,
 * and the whole effect is a function of where that front has reached:
 *
 *  - **behind the front** the tube has been through and is drawn as a thin line
 *    on that same path, brightest just behind the front and easing down to a
 *    readable floor at the tail. A whole L, U or zigzag body is visibly converted
 *    to a filament, not just the last cell of it.
 *  - **ahead of the front** the untouched stretch is drawn through
 *    [ArrowShape.drawTubeOn], which is the arrow's own tube treatment at the
 *    arrow's own widths and alphas, so the part that has not drained yet still
 *    looks exactly like the arrow the player tapped.
 *  - **on the front** the arrowhead rides along as one rigid piece of the drawn
 *    geometry. It is the only thing that turns at all, and only enough to face
 *    the way it is travelling, which is what lets it turn each bend instead of
 *    sliding sideways across it. At the tip the tangent is the final-segment
 *    axis, so the head arrives in exactly its resting orientation. The bright
 *    meniscus sits under it, where thick meets thin.
 *
 * Everything is measured in arc length on [CurvePath], which is the same
 * quadratic Bézier the tube is stroked from, so the head reaches a corner exactly
 * when the flow does and a bend is never jumped.
 *
 * Once the front has reached the tip the entire arrow is a thin line, and [exit]
 * carries that line out along the board's own exit axis, dissolving the trail from
 * the tail forward so the head is the last thing to disappear.
 */
internal fun DrawScope.drawLaunch(
    shape: ArrowShape,
    curve: CurvePath,
    color: Color,
    direction: Direction,
    metrics: BoardMetrics,
    drain: Float,
    exit: Float,
    flicker: Float
) {

    val length = curve.length
    val tube = shape.strokeWidth
    if (length < tube) return

    val flow = drain.coerceIn(0f, 1f)
    val leaving = exit.coerceIn(0f, 1f)

    // Where the flow front has reached, in arc length on the arrow's own path.
    val front = pipeFront(flow, length)

    // The only translation in the effect, and it does not exist until the body has
    // already become a line.
    val travel = metrics.travelFor(direction, shape.tip)
    val shift = Offset(
        x = travel * leaving * direction.columnDelta.toFloat(),
        y = travel * leaving * direction.rowDelta.toFloat()
    )

    // One reused Path for every stretch cut out of the curve, so a frame drawing
    // the collapsed body, the untouched tube and the meniscus allocates nothing.
    val piece = Path()

    withTransform({ translate(shift.x, shift.y) }) {

        drawCollapsedBody(curve, color, tube, front, leaving, flicker, piece)

        if (front < length) {
            curve.fillPath(piece, front, length)
            shape.drawTubeOn(
                scope = this,
                path = piece,
                color = color,
                glow = AheadGlow,
                alpha = aheadAlphaAt(flow)
            )
        }

        drawMeniscus(curve, color, tube, front, flicker, piece)

        val headLight = headLitAt(flow, leaving) * flicker
        if (headLight > 0.01f) {
            shape.drawHeadAt(
                scope = this,
                color = color,
                center = curve.pointAt(front),
                tangent = curve.directionAt(front),
                alpha = headLight,
                glow = HeadGlow + 0.45f * leaving
            )
        }
    }
}

/**
 * The part of the body the flow has already been through, drawn as a thin line.
 *
 * The line is the arrow's own path, never a straight chord and never a re-pathed
 * version of it, so a collapsed L, U or zigzag keeps every corner it had. It is
 * walked in chunks along arc length and each chunk is drawn at the brightness its
 * own position deserves — brightest where the flow just passed, dimmer further
 * back, and never darker than a floor until the line starts leaving the board.
 * Chunking is what makes that ramp wrap round a bend instead of flooding a whole
 * cell evenly.
 *
 * Everything here is narrower than the tube it replaces: the widest layer is a
 * soft halo under three times a hairline, so a draining arrow never looks as
 * thick as the one still ahead of the flow and never spills onto a neighbour.
 */
private fun DrawScope.drawCollapsedBody(
    curve: CurvePath,
    color: Color,
    tube: Float,
    front: Float,
    exit: Float,
    flicker: Float,
    piece: Path
) {
    val length = curve.length
    val start = trailStartAt(front, length, exit)
    if (front <= start) return

    val span = front - start
    val width = collapsedWidth(tube)
    val halo = lerp(StageVoid, NeonCore, 0.55f)

    for (chunk in 0 until TrailChunks) {
        val from = start + span * chunk / TrailChunks
        val to = start + span * (chunk + 1) / TrailChunks
        val lit = trailLitAt(front, length, exit, (from + to) * 0.5f)
        if (lit <= 0.015f) continue

        curve.fillPath(piece, from, to)

        drawPath(
            path = piece,
            color = halo.copy(alpha = (0.13f * lit * flicker).coerceIn(0f, 1f)),
            style = Stroke(
                width = width * 2.8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = piece,
            color = color.copy(alpha = (lit * flicker).coerceIn(0f, 1f)),
            style = Stroke(
                width = width,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = piece,
            color = lerp(color, NeonCore, 0.68f).copy(
                alpha = (0.92f * lit * flicker).coerceIn(0f, 1f)
            ),
            style = Stroke(
                width = width * 0.40f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * The meniscus: the wet, bright lip of liquid sitting on the front.
 *
 * A band right behind the front is drawn thicker and hotter than the collapsed
 * line it grows out of, tapering back to nothing over a fraction of the body, and
 * a soft pool of light is laid under it. Together they cover the step from the
 * full-width tube ahead of the front down to the hairline behind it, so that step
 * reads as the edge of a liquid rather than as a cut in the arrow.
 */
private fun DrawScope.drawMeniscus(
    curve: CurvePath,
    color: Color,
    tube: Float,
    front: Float,
    flicker: Float,
    piece: Path
) {
    if (front <= 0f) return

    val cuff = min(curve.length * 0.15f, tube * 5f)
    val wet = lerp(color, NeonCore, 0.55f)

    for (step in 0 until CuffChunks) {
        val from = front - cuff + cuff * step / CuffChunks
        val to = front - cuff + cuff * (step + 1) / CuffChunks
        val lit = (step + 1f) / CuffChunks
        curve.fillPath(piece, from, to)
        drawPath(
            path = piece,
            color = wet.copy(alpha = (0.45f * lit * flicker).coerceIn(0f, 1f)),
            style = Stroke(
                width = tube * (0.34f + 0.34f * lit),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = piece,
            color = NeonCore.copy(alpha = (0.85f * lit * lit * flicker).coerceIn(0f, 1f)),
            style = Stroke(
                width = tube * 0.22f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    val bead = curve.pointAt(front)
    val reach = tube * 1.5f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to wet.copy(alpha = (0.55f * flicker).coerceIn(0f, 1f)),
                0.45f to color.copy(alpha = (0.22f * flicker).coerceIn(0f, 1f)),
                1f to Color.Transparent
            ),
            center = bead,
            radius = reach
        ),
        radius = reach,
        center = bead
    )
}

/**
 * How far along the arrow's drawn body the flow front has reached, in arc length.
 *
 * The one measurement the whole effect rests on. Because it is a fraction of the
 * body's own length and every draw decision is taken from arc length on the same
 * flattened curve, walking this from 0 to the body's length walks the head
 * through every segment and round every bend.
 */
internal fun pipeFront(flow: Float, length: Float): Float =
    length * flow.coerceIn(0f, 1f)

/**
 * How wide the collapsed body is behind the front, as a fraction of the tube it
 * used to be.
 *
 * The body is never stretched or squeezed: ahead of the front it is the arrow's
 * own tube width and behind the front it is this hairline, and the jump between
 * the two is the front itself.
 */
internal fun collapsedWidth(tubeWidth: Float): Float = tubeWidth * CollapsedFraction

/**
 * Where the collapsed line starts.
 *
 * While the arrow is draining this is the tail, because the line grows out from
 * behind the head. Once it is leaving the board the start walks forward to meet
 * the head, so the line is drawn away from the tail and the head is the last thing
 * on screen.
 */
internal fun trailStartAt(front: Float, length: Float, exit: Float): Float =
    front - length * (1f - exit.coerceIn(0f, 1f))

/**
 * How visible the collapsed line is at [value] arc length.
 *
 * While the arrow is draining this is purely the wetness of the flow: full on the
 * front, easing back to a floor at the tail. The floor is not zero on purpose —
 * the point of the effect is that the whole bent body has visibly become a line,
 * so the far end of that line has to still be there.
 *
 * Once the arrow is leaving the board, [exit] adds the second job: the line is
 * drawn away from its tail end, so the trail dissolves forward and the head is
 * what disappears last.
 */
internal fun trailLitAt(
    front: Float,
    length: Float,
    exit: Float,
    value: Float
): Float {
    val fade = trailFade(length)
    val behind = front - value
    val wet = TrailFloor + (1f - TrailFloor) * (1f - smoothRamp(0f, fade, behind))

    val leaving = exit.coerceIn(0f, 1f)
    if (leaving <= 0f) return wet

    val start = trailStartAt(front, length, leaving)
    return wet * smoothRamp(start, start + fade, value)
}

/**
 * How visible the arrowhead is.
 *
 * It is released from the tail over the first part of the drain, so the head does
 * not pop into existence in a cell that has not been reached yet, and it is faded
 * out over the last part of the exit, so the head is what disappears last.
 */
internal fun headLitAt(flow: Float, exit: Float): Float =
    smoothRamp(0f, Handover, flow.coerceIn(0f, 1f)) *
        (1f - smoothRamp(0.08f, 1f, exit.coerceIn(0f, 1f)))

/**
 * How bright the not-yet-drained stretch of the arrow is.
 *
 * It starts at the arrow's own brightness and settles to [AheadAlpha] over the
 * same window in which the head is released, which reads as the flow taking over
 * the arrow rather than the arrow dimming out from under it.
 */
internal fun aheadAlphaAt(flow: Float): Float =
    1f - (1f - AheadAlpha) * smoothRamp(0f, Handover, flow.coerceIn(0f, 1f))

/**
 * How far the arrowhead has to turn to face [target] instead of [from], in
 * degrees clockwise on screen.
 *
 * The head is turned and never reshaped, so this is the whole of the rotation the
 * effect uses. For a straight arrow the tangent never changes and the result is
 * the same number every frame; for a bent one it walks smoothly through each bend
 * and lands back on zero against the arrow's own axis at the tip.
 */
internal fun headTurnDegrees(from: Offset, target: Offset): Float =
    screenAngle(target) - screenAngle(from)

/** Compass-free screen angle of a vector, in degrees clockwise from +x. */
private fun screenAngle(vector: Offset): Float = if (vector.getDistance() < 0.0001f) {
    0f
} else {
    Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()
}

/**
 * How far back the wetness of the line reaches, in arc length.
 *
 * A fraction of the body so it scales with the arrow: long enough that a whole
 * bend stays bright as the front rounds it, short enough that the tail of a long
 * arrow does not read as the driest part of the flow.
 */
internal fun trailFade(length: Float): Float = max(length * TrailFadeFraction, 1f)

/**
 * A soft 0 to 1 ramp between two edges.
 *
 * Every ramp in the effect is this, which is why none of them can overshoot or
 * step: a bend is lit on exactly the arc length the front has actually reached.
 */
internal fun smoothRamp(edge0: Float, edge1: Float, value: Float): Float {
    if (edge1 <= edge0) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * How many pieces the collapsed line is walked in.
 *
 * Enough that a bend always spans several pieces, so the wetness ramp has to wrap
 * round the corner instead of jumping it, and few enough that one draining arrow
 * stays cheap.
 */
private const val TrailChunks = 16

/** How many pieces the meniscus cuff is ramped in. */
private const val CuffChunks = 3

/** Width of the collapsed line as a fraction of the arrow's own tube width. */
private const val CollapsedFraction = 0.30f

/**
 * How dim the tail of the collapsed line is allowed to get.
 *
 * Not zero on purpose: the point of the effect is that the whole bent body has
 * visibly become a line, so the far end of that line has to still be there.
 */
private const val TrailFloor = 0.38f

/** Reach of the wetness ramp, as a fraction of the body. */
private const val TrailFadeFraction = 0.34f

/** Brightness the not-yet-drained stretch settles to once the flow has taken over. */
private const val AheadAlpha = 0.66f

/** Glow on the not-yet-drained stretch, kept low so the tube never blooms. */
private const val AheadGlow = 0.06f

/** Glow on the travelling head while it is inside the arrow. */
private const val HeadGlow = 0.30f

/** Portion of the drain over which the flow takes the arrow over from rest. */
private const val Handover = 0.10f
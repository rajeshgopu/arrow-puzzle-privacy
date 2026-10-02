package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.StageVoid
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Smooth neon geometry for one complete arrow.
 *
 * The arrow is rendered as:
 *
 *      smooth tube  ───────────────╮
 *                                  ╰──────▶
 *
 * The arrowhead direction is derived from the ACTUAL FINAL SEGMENT,
 * so it always points exactly in the direction in which the arrow exits.
 */
internal class ArrowShape(
    tile: ArrowTile,
    cellWidth: Float,
    cellHeight: Float,
    gap: Float,
    left: Float = 0f,
    top: Float = 0f
) {

    /**
     * The scale this arrow was actually built at: the smaller of its two cell
     * edges, in pixels.
     *
     * Every launch effect multiplies its own sizes by this, so nothing has to
     * know the board size, the density, the level's dimensions or whether the
     * window is portrait or landscape — the arrow is measured, not assumed.
     */
    internal val unit = min(cellWidth, cellHeight)

    /** Main neon tube width. */
    val strokeWidth = unit * 0.088f

    /** Touch tolerance around the tube. */
    val hitRadius = unit * 0.40f

    /** Rounded bend radius. */
    private val bendRadius = unit * 0.42f

    /** Arrowhead dimensions. */
    private val headLength = unit * 0.28f
    private val headWidth = unit * 0.20f

    /** Small overlap between tube and arrowhead. */
    private val headOverlap = unit * 0.025f

    /**
     * How far the glow reaches beyond the tube, as a fraction of the tube width.
     *
     * The neon is lit from inside the glass, so the light stays glued to the
     * tube: the soft layer lifts the edge by a fraction of a stroke and the
     * tight layer only adds a bright rim. Nothing here is wide enough to form a
     * halo or to bleed into a neighbouring arrow.
     */
    private val glowSpreadSoft = strokeWidth * 0.30f
    private val glowSpreadTight = strokeWidth * 0.08f

    /** Half-width the dark separation keyline adds on each side of the tube. */
    private val keylineExtra = unit * 0.075f

    /** Extra tail extension. */
    private val tailReach = max(cellWidth, cellHeight) * 0.5f

    /** Body geometry. */
    val shaft: Path

    /** Main arrowhead. */
    private val head: Path

    /** Bright inner arrowhead. */
    private val headCore: Path

    /** Arrowhead glow layers. */
    private val headGlows: List<HeadGlow>

    /**
     * The axis the resting arrowhead points along, taken from the arrow's final
     * segment. A launch that drives the head along the body turns it relative to
     * this, so it needs the axis itself rather than a baked-in angle.
     */
    internal val headAxis: Offset

    /**
     * Centroid of the arrowhead outline. This is the point the head is turned
     * about when a launch walks it along the body, so the head rotates about its
     * own middle instead of swinging out from one corner.
     */
    internal val headPivot: Offset

    /**
     * One halo-free glow rim drawn behind an arrowhead: the arrowhead scaled
     * only slightly outwards from its connection point, so the light hugs the
     * head instead of floating around it.
     */
    private class HeadGlow(
        val path: Path,
        val alpha: Float
    )

    /** Spine points used for hit testing. */
    val spine: List<Offset>

    /** Exact arrow tip. */
    val tip: Offset

    /** Arrow pivot. */
    val pivot: Offset

    init {

        // [left]/[top] place the grid inside a plate that can be larger than the
        // grid itself, so a board centred on a non-matching play area still
        // draws every cell where the maze expects it.
        fun center(position: BoardPosition): Offset {
            return Offset(
                x = left + position.column * (cellWidth + gap) + cellWidth / 2f,
                y = top + position.row * (cellHeight + gap) + cellHeight / 2f
            )
        }

        val points = tile.cells.map(::center)

        /*
         * ------------------------------------------------------------
         * DETERMINE FINAL SEGMENT DIRECTION
         * ------------------------------------------------------------
         *
         * Do NOT use tile.direction for the visual arrowhead orientation.
         *
         * For a multi-cell arrow:
         *
         *       P(n-1) ───────▶ P(n)
         *
         * direction = P(n) - P(n-1)
         *
         * This guarantees the arrowhead follows the actual final segment.
         */
        val finalDirection: Offset

        if (points.size >= 2) {
            val last = points.last()
            val previous = points[points.lastIndex - 1]

            val segment = last - previous

            finalDirection =
                if (segment.getDistance() > 0.01f) {
                    segment.unit()
                } else {
                    tile.direction.unitVector().toOffset()
                }
        } else {
            finalDirection = tile.direction.unitVector().toOffset()
        }

        val fx = finalDirection.x
        val fy = finalDirection.y

        /*
         * Perpendicular vector.
         *
         * If direction is RIGHT:
         *   direction = (1, 0)
         *   perpendicular = (0, 1)
         */
        val perp = Offset(
            x = -fy,
            y = fx
        )

        /*
         * ------------------------------------------------------------
         * BODY VERTICES
         * ------------------------------------------------------------
         */

        val headCenter = points.last()

        /*
         * The tube terminates slightly inside the head.
         *
         * This allows the arrowhead to visually merge with the tube.
         */
        val base = Offset(
            x = headCenter.x - fx * headOverlap,
            y = headCenter.y - fy * headOverlap
        )

        val vertices = ArrayList<Offset>(points.size + 1)

        if (points.size == 1) {

            vertices += Offset(
                x = base.x - fx * tailReach,
                y = base.y - fy * tailReach
            )

        } else {

            val first = points.first()
            val second = points[1]

            val tailDirection = (first - second).unit()

            vertices += Offset(
                x = first.x + tailDirection.x * tailReach,
                y = first.y + tailDirection.y * tailReach
            )

            for (index in 0 until points.size - 1) {
                vertices += points[index]
            }
        }

        /*
         * The final point is the arrowhead connection point.
         */
        vertices += base

        spine = vertices

        /*
         * Build the smooth body.
         */
        shaft = smoothPolyline(
            points = vertices,
            radius = bendRadius
        )

        /*
         * ------------------------------------------------------------
         * ARROWHEAD
         * ------------------------------------------------------------
         *
         * The arrowhead is mathematically aligned to:
         *
         *     FINAL POINT - PREVIOUS POINT
         *
         * and therefore automatically supports:
         *
         * RIGHT ▶
         * LEFT  ◀
         * UP    ▲
         * DOWN  ▼
         *
         * without relying on screen orientation.
         */

        val connection = Offset(
            x = base.x - fx * headOverlap,
            y = base.y - fy * headOverlap
        )

        /*
         * Tip lies directly on the continuation of the final segment.
         */
        val tipPoint = Offset(
            x = connection.x + fx * headLength,
            y = connection.y + fy * headLength
        )

        /*
         * Narrow neck where the tube enters the arrowhead.
         *
         * This is intentionally close to the tube width so the
         * arrowhead does not look like a separate triangle.
         */
        val neckHalfWidth = strokeWidth * 0.62f

        val neckA = Offset(
            x = connection.x + perp.x * neckHalfWidth,
            y = connection.y + perp.y * neckHalfWidth
        )

        val neckB = Offset(
            x = connection.x - perp.x * neckHalfWidth,
            y = connection.y - perp.y * neckHalfWidth
        )

        /*
         * Wings flare outward slightly in front of the neck.
         */
        val wingCenter = Offset(
            x = connection.x + fx * headLength * 0.32f,
            y = connection.y + fy * headLength * 0.32f
        )

        val wingHalfWidth = headWidth * 0.5f

        val wingA = Offset(
            x = wingCenter.x + perp.x * wingHalfWidth,
            y = wingCenter.y + perp.y * wingHalfWidth
        )

        val wingB = Offset(
            x = wingCenter.x - perp.x * wingHalfWidth,
            y = wingCenter.y - perp.y * wingHalfWidth
        )

        /*
         * Arrowhead polygon:
         *
         *                TIP
         *                 ▲
         *                / \
         *          wing /   \ wing
         *              /     \
         *          neck       neck
         *             \       /
         *              ───────
         *
         * The neck overlaps the tube slightly.
         */
        val headOutline = listOf(
            tipPoint,
            wingA,
            neckA,
            neckB,
            wingB
        )

        head = kitePath(headOutline)

        headAxis = Offset(x = fx, y = fy)
        headPivot = centroidOf(headOutline)

        /*
         * Smaller bright core.
         */
        val coreTip = Offset(
            x = connection.x + fx * headLength * 0.88f,
            y = connection.y + fy * headLength * 0.88f
        )

        val coreWingCenter = Offset(
            x = connection.x + fx * headLength * 0.30f,
            y = connection.y + fy * headLength * 0.30f
        )

        val coreWingHalf = headWidth * 0.25f
        val coreNeckHalf = strokeWidth * 0.40f

        val coreWingA = Offset(
            x = coreWingCenter.x + perp.x * coreWingHalf,
            y = coreWingCenter.y + perp.y * coreWingHalf
        )

        val coreWingB = Offset(
            x = coreWingCenter.x - perp.x * coreWingHalf,
            y = coreWingCenter.y - perp.y * coreWingHalf
        )

        val coreNeckA = Offset(
            x = connection.x + perp.x * coreNeckHalf,
            y = connection.y + perp.y * coreNeckHalf
        )

        val coreNeckB = Offset(
            x = connection.x - perp.x * coreNeckHalf,
            y = connection.y - perp.y * coreNeckHalf
        )

        headCore = kitePath(
            listOf(
                coreTip,
                coreWingA,
                coreNeckA,
                coreNeckB,
                coreWingB
            )
        )

        /*
         * ------------------------------------------------------------
         * HEAD GLOW
         * ------------------------------------------------------------
         *
         * Two rims that sit just outside the arrowhead. They are scaled
         * around the neck, so the extra light is a thin fringe along the
         * head rather than a separate shape floating behind it.
         */

        headGlows = listOf(
            1.13f to 0.07f,
            1.06f to 0.20f
        ).map { (factor, alpha) ->

            val glowTip = Offset(
                x = connection.x + fx * headLength * factor,
                y = connection.y + fy * headLength * factor
            )

            val glowWingCenter = Offset(
                x = connection.x + fx * headLength * 0.32f * factor,
                y = connection.y + fy * headLength * 0.32f * factor
            )

            val glowWingHalf = headWidth * 0.5f * factor

            val glowWingA = Offset(
                x = glowWingCenter.x + perp.x * glowWingHalf,
                y = glowWingCenter.y + perp.y * glowWingHalf
            )

            val glowWingB = Offset(
                x = glowWingCenter.x - perp.x * glowWingHalf,
                y = glowWingCenter.y - perp.y * glowWingHalf
            )

            val glowNeckHalf = strokeWidth * 0.62f * factor

            val glowNeckA = Offset(
                x = connection.x + perp.x * glowNeckHalf,
                y = connection.y + perp.y * glowNeckHalf
            )

            val glowNeckB = Offset(
                x = connection.x - perp.x * glowNeckHalf,
                y = connection.y - perp.y * glowNeckHalf
            )

            HeadGlow(
                path = kitePath(
                    listOf(
                        glowTip,
                        glowWingA,
                        glowNeckA,
                        glowNeckB,
                        glowWingB
                    )
                ),
                alpha = alpha
            )
        }

        /*
         * ------------------------------------------------------------
         * PIVOT / BOUNDS
         * ------------------------------------------------------------
         */

        val minX = vertices.minOf { it.x }
        val maxX = vertices.maxOf { it.x }
        val minY = vertices.minOf { it.y }
        val maxY = vertices.maxOf { it.y }

        pivot = Offset(
            x = (minX + maxX) / 2f,
            y = (minY + maxY) / 2f
        )

        tip = tipPoint
    }

    /**
     * Distance from a touch point to the arrow spine.
     */
    fun distanceTo(
        x: Float,
        y: Float
    ): Float {

        if (spine.size == 1) {
            return (Offset(x, y) - spine[0]).getDistance()
        }

        var best = Float.MAX_VALUE

        for (index in 0 until spine.size - 1) {

            val distance = segmentDistance(
                x,
                y,
                spine[index],
                spine[index + 1]
            )

            if (distance < best) {
                best = distance
            }
        }

        return best
    }

    /**
     * Draw neon arrow.
     *
     * The arrow is drawn whole, in one piece, exactly as it was built: the tube
     * from its own path at its own widths and the head from its own path in its
     * own place, both only moved as far as [translation] and [scale] say.
     */
    fun draw(
        scope: DrawScope,
        color: Color,
        glow: Float = 0f,
        translation: Offset = Offset.Zero,
        scale: Float = 1f,
        alpha: Float = 1f
    ) {

        val opacity = alpha.coerceIn(0f, 1f)

        scope.withTransform({

            if (
                translation.x != 0f ||
                translation.y != 0f
            ) {
                translate(
                    translation.x,
                    translation.y
                )
            }

            if (scale != 1f) {
                scale(
                    scale,
                    scale,
                    pivot
                )
            }

        }) {
            drawTubeOn(this, shaft, color, glow, opacity)
            drawHeadOn(color, opacity, launchEnergy(glow))
        }
    }

    /**
     * Draws this arrow's own tube treatment — dark keyline, both glow rims, the
     * neon body and the white-hot core — over [path] instead of over the whole
     * [shaft].
     *
     * [path] only has to follow the same geometry as [shaft]; a slice of it is
     * enough. Every width, colour and alpha here is the resting arrow's, so the
     * result is the same tube, drawn over a shorter stretch of its own path. That
     * is what lets a launch show the not-yet-flowed part of a bent arrow as the
     * arrow itself, at its own width, with no second styling to drift out of
     * step.
     */
    internal fun drawTubeOn(
        scope: DrawScope,
        path: Path,
        color: Color,
        glow: Float = 0f,
        alpha: Float = 1f
    ) {
        val opacity = alpha.coerceIn(0f, 1f)
        if (opacity <= 0.01f) return

        val energy = launchEnergy(glow)

        // Dark separation keyline, so the tube never bleeds into a neighbour.
        scope.drawPath(
            path = path,
            color = StageVoid.copy(alpha = 0.92f * opacity),
            style = Stroke(
                width = strokeWidth + keylineExtra,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Soft ambient glow, then the tight rim: the same two rims the resting
        // arrow has, at the same spreads.
        scope.tubeGlow(path, glowSpreadSoft, 0.09f * energy, opacity)
        scope.tubeGlow(path, glowSpreadTight, 0.34f * energy, opacity)

        scope.drawPath(
            path = path,
            color = color.copy(alpha = opacity),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        scope.drawPath(
            path = path,
            color = lerp(color, NeonCore, 0.62f).copy(alpha = opacity),
            style = Stroke(
                width = strokeWidth * 0.34f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    /**
     * Draws this arrow's arrowhead as one rigid piece of its own geometry, moved
     * to [center] and turned to point along [tangent].
     *
     * Nothing is scaled, sheared or re-pathed: the same kite with the same neck,
     * the same bright core and the same two glow rims, translated and turned as a
     * unit about [headPivot]. The head is the only thing a launch ever turns, and
     * only enough to face the way it is travelling — which is what lets it lead a
     * flow through a bend instead of sliding sideways across it. At the end of the
     * body the tangent is the final-segment axis, so [tangent] equals [headAxis]
     * and the head comes to rest in exactly its drawn orientation.
     */
    internal fun drawHeadAt(
        scope: DrawScope,
        color: Color,
        center: Offset,
        tangent: Offset,
        alpha: Float = 1f,
        glow: Float = 0f
    ) {
        val opacity = alpha.coerceIn(0f, 1f)
        if (opacity <= 0.01f) return

        val turn = headTurnDegrees(headAxis, tangent)

        scope.withTransform({
            translate(center.x - headPivot.x, center.y - headPivot.y)
            rotate(degrees = turn, pivot = headPivot)
        }) {
            drawHeadOn(color, opacity, launchEnergy(glow))
        }
    }

    /**
     * The glow layers of the arrowhead, drawn from its own paths in their own
     * place. Nothing here moves, resizes or reshapes the head: [energy] is the same
     * 1 to 1.8 launch band the tube uses and only lifts its brightness.
     */
    private fun DrawScope.drawHeadOn(
        color: Color,
        opacity: Float,
        energy: Float
    ) {
        val halo = lerp(StageVoid, NeonCore, 0.55f)

        for (rim in headGlows) {
            drawPath(
                path = rim.path,
                color = halo.copy(
                    alpha = (rim.alpha * energy * opacity).coerceIn(0f, 1f)
                )
            )
        }

        drawPath(path = head, color = color.copy(alpha = opacity))
        drawPath(path = headCore, color = NeonCore.copy(alpha = 0.50f * opacity))
    }

    /**
     * Draw a glow following the exact same curved shaft.
     *
     * The tint is kept pale and the alpha capped, so the layer only ever reads as
     * light spilling a short distance past the tube edge.
     */
    private fun DrawScope.tubeGlow(
        path: Path,
        extra: Float,
        alpha: Float,
        opacity: Float
    ) {

        val scaled = (alpha * opacity).coerceIn(0f, 0.5f)

        if (scaled <= 0.01f) {
            return
        }

        val tint = lerp(StageVoid, NeonCore, 0.55f).copy(alpha = scaled)

        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth + extra,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * How far the launch glow lifts the tube and the head.
 *
 * Energy drives brightness only. It is capped well below the point where the
 * glow would bloom into fog, so a draining arrow brightens without ever growing a
 * halo or touching a neighbour.
 */
private fun launchEnergy(glow: Float): Float = (1f + glow).coerceIn(1f, 1.8f)

/** Mean position of a closed outline, used as the arrowhead's own turning point. */
private fun centroidOf(points: List<Offset>): Offset = Offset(
    x = points.sumOf { it.x.toDouble() }.toFloat() / points.size,
    y = points.sumOf { it.y.toDouble() }.toFloat() / points.size
)

/**
 * Creates a smooth continuous path.
 *
 * Every real direction change is converted into a rounded quadratic
 * Bézier curve instead of a sharp 90-degree corner.
 */
private fun smoothPolyline(
    points: List<Offset>,
    radius: Float
): Path {

    val path = Path()

    if (points.isEmpty()) {
        return path
    }

    if (points.size == 1) {
        path.moveTo(
            points[0].x,
            points[0].y
        )
        return path
    }

    path.moveTo(
        points.first().x,
        points.first().y
    )

    for (index in 1 until points.size - 1) {

        val previous = points[index - 1]
        val corner = points[index]
        val next = points[index + 1]

        val incoming = corner - previous
        val outgoing = next - corner

        val incomingLength = incoming.getDistance()
        val outgoingLength = outgoing.getDistance()

        if (
            incomingLength < 0.01f ||
            outgoingLength < 0.01f
        ) {
            continue
        }

        val inDir = incoming.unit()
        val outDir = outgoing.unit()

        /*
         * Cross product tells whether the direction actually changes.
         */
        val cross =
            inDir.x * outDir.y -
            inDir.y * outDir.x

        /*
         * Dot product identifies a straight continuation.
         */
        val dot =
            inDir.x * outDir.x +
            inDir.y * outDir.y

        /*
         * Straight line.
         */
        if (
            abs(cross) < 0.01f &&
            dot > 0f
        ) {
            path.lineTo(
                corner.x,
                corner.y
            )
            continue
        }

        /*
         * Limit the radius so neighbouring bends do not overlap.
         */
        val r = min(
            radius,
            min(
                incomingLength * 0.42f,
                outgoingLength * 0.42f
            )
        )

        /*
         * Point where the straight incoming segment stops.
         */
        val before = Offset(
            x = corner.x - inDir.x * r,
            y = corner.y - inDir.y * r
        )

        /*
         * Point where the outgoing straight segment begins.
         */
        val after = Offset(
            x = corner.x + outDir.x * r,
            y = corner.y + outDir.y * r
        )

        /*
         * Straight line into the bend.
         */
        path.lineTo(
            before.x,
            before.y
        )

        /*
         * Quadratic Bézier:
         *
         * before ----\
         *              )
         *             /
         * after -----
         *
         * The corner itself is the control point.
         *
         * This produces a smooth rounded bend with tangent-aligned
         * entry and exit.
         */
        path.quadraticTo(
            corner.x,
            corner.y,
            after.x,
            after.y
        )
    }

    /*
     * Final straight segment.
     */
    path.lineTo(
        points.last().x,
        points.last().y
    )

    return path
}

/**
 * Creates a closed polygon.
 */
private fun kitePath(
    points: List<Offset>
): Path {

    val path = Path()

    if (points.isEmpty()) {
        return path
    }

    path.moveTo(
        points[0].x,
        points[0].y
    )

    for (index in 1 until points.size) {

        path.lineTo(
            points[index].x,
            points[index].y
        )
    }

    path.close()

    return path
}

/**
 * Distance from a point to a line segment.
 */
private fun segmentDistance(
    px: Float,
    py: Float,
    a: Offset,
    b: Offset
): Float {

    val dx = b.x - a.x
    val dy = b.y - a.y

    val lengthSquared =
        dx * dx + dy * dy

    if (lengthSquared < 0.0001f) {

        return (
            Offset(px, py) - a
        ).getDistance()
    }

    val t = (
        (
            (px - a.x) * dx +
            (py - a.y) * dy
        ) / lengthSquared
    ).coerceIn(
        0f,
        1f
    )

    val closest = Offset(
        x = a.x + dx * t,
        y = a.y + dy * t
    )

    return (
        Offset(px, py) - closest
    ).getDistance()
}

/**
 * Normalised vector.
 */
private fun Offset.unit(): Offset {

    val length = getDistance()

    return if (length < 0.01f) {
        Offset.Zero
    } else {
        this * (1f / length)
    }
}

/**
 * Converts Direction to an Offset.
 */
private fun Pair<Float, Float>.toOffset(): Offset {
    return Offset(
        x = first,
        y = second
    )
}

/**
 * Direction vector.
 */
private fun Direction.unitVector(): Pair<Float, Float> {
    return when (this) {
        Direction.RIGHT -> 1f to 0f
        Direction.LEFT -> -1f to 0f
        Direction.UP -> 0f to -1f
        Direction.DOWN -> 0f to 1f
    }
}
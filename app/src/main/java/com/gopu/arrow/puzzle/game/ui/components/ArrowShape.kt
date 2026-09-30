package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction

/**
 * Board-space geometry for one arrow, computed from its input coordinates.
 *
 * The body is a thick, rounded polyline through the tile's cell centres; each
 * cell it crosses is filled evenly and every turn gets a clear rounded curve.
 * The body stops at the head base and the head is a triangle that continues in
 * the exit [ArrowTile.direction], which always matches the final body segment
 * (the engine guarantees this), so the two never overlap or double back.
 */
internal class ArrowShape(
    tile: ArrowTile,
    cellWidth: Float,
    cellHeight: Float,
    gap: Float
) {
    private val unit = minOf(cellWidth, cellHeight)
    val strokeWidth = unit * 0.09f
    private val headHalf = unit * 0.15f
    private val tipAdvance = unit * 0.20f
    private val baseBack = unit * 0.04f
    private val tailReach = unit * 0.26f
    private val bendRadius = unit * 0.15f
    private val outline = strokeWidth * 0.6f

    val shaft: Path
    val head: Path

    init {
        fun center(position: BoardPosition): Offset = Offset(
            x = position.column * (cellWidth + gap) + cellWidth / 2f,
            y = position.row * (cellHeight + gap) + cellHeight / 2f
        )

        val points = tile.cells.map(::center)
        val headCenter = points.last()
        val (fx, fy) = tile.direction.unitVector()

        val headBase = Offset(
            headCenter.x - fx * baseBack,
            headCenter.y - fy * baseBack
        )

        // Body vertices: start at the outer edge of the tail cell, run through
        // the cells, and stop at the head base. The head cell centre is skipped
        // because the exit direction already points past it, so keeping it would
        // make the body double back into its own arrowhead.
        val vertices = ArrayList<Offset>(points.size + 1)
        if (tile.cells.size == 1) {
            vertices += Offset(
                headCenter.x - fx * tailReach,
                headCenter.y - fy * tailReach
            )
        } else {
            vertices += points.first() + (points.first() - points[1]).unit() * tailReach
            for (index in 0 until points.size - 1) vertices += points[index]
        }
        vertices += headBase

        shaft = roundedPath(vertices, bendRadius)

        val tip = Offset(
            headCenter.x + fx * tipAdvance,
            headCenter.y + fy * tipAdvance
        )
        val perpX = -fy * headHalf
        val perpY = fx * headHalf
        head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(headBase.x + perpX, headBase.y + perpY)
            lineTo(headBase.x - perpX, headBase.y - perpY)
            close()
        }
    }

    /** Draws the arrow shaft then its head in a single [color]. */
    fun draw(scope: DrawScope, color: Color) {
        // Black edge pass first, then the coloured body on top, so every arrow
        // reads as a solid shape with a crisp dark outline.
        scope.drawPath(
            shaft,
            Color.Black,
            style = Stroke(width = strokeWidth + outline * 2f, cap = StrokeCap.Butt, join = StrokeJoin.Round)
        )
        scope.drawPath(
            head,
            Color.Black,
            style = Stroke(width = outline * 2f, join = StrokeJoin.Round)
        )
        scope.drawPath(
            shaft,
            color,
            // Flat tail; the head end is hidden behind the arrowhead triangle.
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt, join = StrokeJoin.Round)
        )
        scope.drawPath(head, color)
    }
}

/** Builds a polyline with rounded corners from [points]. */
private fun roundedPath(points: List<Offset>, radius: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (index in 1 until points.size - 1) {
        val previous = points[index - 1]
        val corner = points[index]
        val next = points[index + 1]
        val incoming = corner - previous
        val outgoing = next - corner
        val incomingLength = incoming.getDistance()
        val outgoingLength = outgoing.getDistance()
        if (incomingLength < 0.01f || outgoingLength < 0.01f) continue
        val r = minOf(radius, incomingLength / 2f, outgoingLength / 2f)
        val before = corner - incoming * (r / incomingLength)
        val after = corner + outgoing * (r / outgoingLength)
        path.lineTo(before.x, before.y)
        path.quadraticTo(corner.x, corner.y, after.x, after.y)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

private fun Offset.unit(): Offset {
    val length = getDistance()
    return if (length < 0.01f) Offset.Zero else this * (1f / length)
}

/** Unit vector pointing along [this] direction. */
private fun Direction.unitVector(): Pair<Float, Float> = when (this) {
    Direction.RIGHT -> 1f to 0f
    Direction.LEFT -> -1f to 0f
    Direction.UP -> 0f to -1f
    Direction.DOWN -> 0f to 1f
}

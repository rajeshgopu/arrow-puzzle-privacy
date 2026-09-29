package com.gopu.arrowpuzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gopu.arrowpuzzle.game.ArrowTile
import com.gopu.arrowpuzzle.game.BoardPosition
import com.gopu.arrowpuzzle.game.Direction
import com.gopu.arrowpuzzle.game.GameStatus
import com.gopu.arrowpuzzle.game.PuzzleState
import com.gopu.arrowpuzzle.ui.pulse
import com.gopu.arrowpuzzle.ui.shake
import com.gopu.arrowpuzzle.ui.theme.Coral
import com.gopu.arrowpuzzle.ui.theme.FlameOrange
import com.gopu.arrowpuzzle.ui.theme.FlameYellow
import com.gopu.arrowpuzzle.ui.theme.Gold
import com.gopu.arrowpuzzle.ui.theme.GridDot
import com.gopu.arrowpuzzle.ui.theme.NeonAmber
import com.gopu.arrowpuzzle.ui.theme.NeonCyan
import com.gopu.arrowpuzzle.ui.theme.NeonLime
import com.gopu.arrowpuzzle.ui.theme.NeonMagenta
import com.gopu.arrowpuzzle.ui.theme.NeonViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CellGap = 3.dp

/** A transient signal that a specific tile should shake (invalid tap). */
data class BoardPulse(val position: BoardPosition, val count: Int)

/** A short fire/rocket exhaust shown where an arrow just launched off the board. */
data class TileBurst(val position: BoardPosition, val direction: Direction, val id: Long)

/** What kind of assist ring, if any, is drawn on the highlighted arrow head. */
enum class TileHighlight { NONE, TUTORIAL, HINT }

/**
 * Arrow board. Arrows are drawn in a single overlay canvas as continuous neon
 * polylines (so bent arrows have no gaps at cell seams), while an invisible grid
 * of cells above them handles taps, the assist ring, the invalid shake, and the
 * launch flame. The board itself has a transparent background.
 */
@Composable
fun ArrowBoard(
    state: PuzzleState,
    modifier: Modifier = Modifier,
    inputEnabled: Boolean = true,
    canTap: (BoardPosition) -> Boolean = { true },
    highlightPosition: BoardPosition? = null,
    highlight: TileHighlight = TileHighlight.NONE,
    pulse: BoardPulse? = null,
    pulseCount: Int = 0,
    bursts: List<TileBurst> = emptyList(),
    onArrowTap: (BoardPosition) -> Unit
) {
    val tiles = state.level.tiles
    val entrances = remember(state.level.id) { List(tiles.size) { Animatable(0f) } }
    LaunchedEffect(state.level.id) {
        entrances.forEachIndexed { index, anim ->
            launch {
                delay((index * 22L).coerceAtMost(800L))
                anim.animateTo(1f, tween(240))
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawEmptyCellDots(state)
            drawArrows(state, entrances)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(CellGap)
        ) {
            repeat(state.level.height) { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CellGap)
                ) {
                    repeat(state.level.width) { column ->
                        val position = BoardPosition(row, column)
                        val tile = state.level.tileAt(position)
                        val isRemaining = position in state.remainingTiles
                        val isHighlighted = highlight != TileHighlight.NONE && position == highlightPosition
                        val isInvalidTarget = pulse?.position == position
                        val tileInputEnabled = inputEnabled && canTap(position) && state.status == GameStatus.PLAYING
                        val burst = bursts.firstOrNull { it.position == position }

                        val ringColor = when {
                            highlight == TileHighlight.HINT -> Gold
                            highlight == TileHighlight.TUTORIAL -> NeonAmber
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .shake(if (isInvalidTarget) pulse?.count ?: 0 else 0)
                                .pulse(if (isHighlighted && highlight == TileHighlight.HINT) pulseCount else 0)
                                .then(
                                    if (isHighlighted && ringColor != null) {
                                        Modifier.border(3.dp, ringColor, RoundedCornerShape(14.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable(
                                    enabled = isRemaining && tileInputEnabled,
                                    onClickLabel = "Remove arrow",
                                    onClick = { onArrowTap(position) }
                                )
                                .then(
                                    if (isRemaining && tile != null) {
                                        Modifier.semantics {
                                            contentDescription = "Arrow pointing ${tile.direction.name.lowercase()}, row ${row + 1}, column ${column + 1}"
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            if (burst != null) {
                                TileFireBurst(direction = burst.direction, trigger = burst.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a small dot at the centre of every cell that no remaining arrow owns,
 * so a grid of dots is revealed as arrows launch off the board.
 */
private fun DrawScope.drawEmptyCellDots(state: PuzzleState) {
    val cols = state.level.width
    val rows = state.level.height
    if (cols == 0 || rows == 0) return

    val gap = CellGap.toPx()
    val cellW = (size.width - (cols - 1) * gap) / cols
    val cellH = (size.height - (rows - 1) * gap) / rows
    val radius = minOf(cellW, cellH) * 0.035f

    for (row in 0 until rows) {
        for (column in 0 until cols) {
            val position = BoardPosition(row, column)
            if (position in state.remainingTiles) continue
            val center = Offset(
                x = column * (cellW + gap) + cellW / 2f,
                y = row * (cellH + gap) + cellH / 2f
            )
            drawCircle(color = GridDot, radius = radius, center = center)
        }
    }
}

private fun DrawScope.drawArrows(state: PuzzleState, entrances: List<Animatable<Float, *>>) {
    val cols = state.level.width
    val rows = state.level.height
    if (cols == 0 || rows == 0) return

    val gap = CellGap.toPx()
    val cellW = (size.width - (cols - 1) * gap) / cols
    val cellH = (size.height - (rows - 1) * gap) / rows

    state.level.tiles.forEachIndexed { index, tile ->
        if (tile.cells.first() !in state.remainingTiles) return@forEachIndexed
        val alpha = (entrances.getOrNull(index)?.value ?: 1f).coerceIn(0f, 1f)
        if (alpha <= 0.01f) return@forEachIndexed

        ArrowShape(
            tile = tile,
            cellWidth = cellW,
            cellHeight = cellH,
            gap = gap
        ).draw(this, tileColor(index).copy(alpha = alpha))
    }
}

/** Rocket exhaust that kicks backwards from the cell an arrow just left. */
@Composable
private fun TileFireBurst(direction: Direction, trigger: Long) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(380))
    }
    val p = anim.value
    if (p <= 0f || p >= 1f) return
    val rotation = when (direction) {
        Direction.RIGHT -> 90f
        Direction.LEFT -> -90f
        Direction.UP -> 0f
        Direction.DOWN -> 180f
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val trail = 34f * p
                translationX = -direction.columnDelta * trail
                translationY = -direction.rowDelta * trail
                rotationZ = rotation
                alpha = (1f - p).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        RocketFlame(
            intensity = (1f - p).coerceIn(0f, 1f),
            modifier = Modifier.size(24.dp, 40.dp)
        )
    }
}

fun tileColor(index: Int): Color = when (index % 5) {
    0 -> NeonCyan
    1 -> NeonMagenta
    2 -> NeonLime
    3 -> NeonAmber
    else -> NeonViolet
}

/**
 * A standalone arrow glyph (used by Home/Splash). Straight arrows and the
 * arrowhead of bent arrows share this geometry.
 */
@Composable
fun ArrowMark(
    modifier: Modifier,
    direction: Direction = Direction.RIGHT,
    color: Color = Color.White,
    glow: Boolean = false
) {
    Canvas(modifier = modifier) {
        val path = buildArrowPath(direction)
        if (glow) {
            val pivot = Offset(size.width / 2f, size.height / 2f)
            for (ring in 3 downTo 1) {
                scale(1f + ring * 0.07f, pivot) {
                    drawPath(path, color.copy(alpha = 0.12f))
                }
            }
        }
        drawPath(path, color)
    }
}

private fun DrawScope.buildArrowPath(direction: Direction): Path {
    val center = Offset(size.width / 2f, size.height / 2f)
    val shaft = size.minDimension * 0.30f
    val head = size.minDimension * 0.22f
    val path = Path()

    fun point(x: Float, y: Float): Offset = when (direction) {
        Direction.RIGHT -> Offset(center.x + x, center.y + y)
        Direction.LEFT -> Offset(center.x - x, center.y + y)
        Direction.UP -> Offset(center.x + y, center.y - x)
        Direction.DOWN -> Offset(center.x + y, center.y + x)
    }

    path.moveTo(point(-shaft, -head / 2f).x, point(-shaft, -head / 2f).y)
    path.lineTo(point(shaft * 0.35f, -head / 2f).x, point(shaft * 0.35f, -head / 2f).y)
    path.lineTo(point(shaft * 0.35f, -head).x, point(shaft * 0.35f, -head).y)
    path.lineTo(point(shaft, 0f).x, point(shaft, 0f).y)
    path.lineTo(point(shaft * 0.35f, head).x, point(shaft * 0.35f, head).y)
    path.lineTo(point(shaft * 0.35f, head / 2f).x, point(shaft * 0.35f, head / 2f).y)
    path.lineTo(point(-shaft, head / 2f).x, point(-shaft, head / 2f).y)
    path.close()
    return path
}

/**
 * Animated exhaust flame drawn below a launching arrow. [intensity] drives both
 * length and brightness so it can be tied to a launch animation.
 */
@Composable
fun RocketFlame(
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val flicker by rememberInfiniteTransition(label = "flame").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(120), RepeatMode.Reverse),
        label = "flicker"
    )
    Canvas(modifier = modifier) {
        if (intensity <= 0.01f) return@Canvas
        val w = size.width
        val h = size.height
        val cx = w / 2f

        fun tongue(color: Color, widthScale: Float, lengthScale: Float, alpha: Float) {
            val length = (h * lengthScale * intensity * flicker).coerceAtLeast(0f)
            if (length <= 0f) return
            val path = Path()
            path.moveTo(cx, 0f)
            path.cubicTo(cx + w * 0.5f * widthScale, h * 0.30f, cx + w * 0.30f * widthScale, length, cx, length)
            path.cubicTo(cx - w * 0.30f * widthScale, length, cx - w * 0.5f * widthScale, h * 0.30f, cx, 0f)
            path.close()
            drawPath(path, color.copy(alpha = (alpha * intensity).coerceIn(0f, 1f)))
        }

        tongue(FlameOrange, 1f, 1f, 0.95f)
        tongue(FlameYellow, 0.7f, 0.78f, 0.95f)
        tongue(Color.White, 0.4f, 0.5f, 1f)
    }
}

package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.GameStatus
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.PuzzleState
import com.gopu.arrow.puzzle.game.ui.theme.BoardFrame
import com.gopu.arrow.puzzle.game.ui.theme.BoardPlate
import com.gopu.arrow.puzzle.game.ui.theme.BoardPlateEdge
import com.gopu.arrow.puzzle.game.ui.theme.GridDotDim
import com.gopu.arrow.puzzle.game.ui.theme.NeonAmber
import com.gopu.arrow.puzzle.game.ui.theme.NeonBlue
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.NeonGreen
import com.gopu.arrow.puzzle.game.ui.theme.NeonLime
import com.gopu.arrow.puzzle.game.ui.theme.NeonMagenta
import com.gopu.arrow.puzzle.game.ui.theme.NeonOrange
import com.gopu.arrow.puzzle.game.ui.theme.NeonRed
import com.gopu.arrow.puzzle.game.ui.theme.NeonTextDim
import com.gopu.arrow.puzzle.game.ui.theme.NeonViolet
import com.gopu.arrow.puzzle.game.ui.theme.StageDeep
import com.gopu.arrow.puzzle.game.ui.theme.StageVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Gap between cells, kept tight so a dense maze still fills the plate. */
private val CellGap = 2.5.dp

/**
 * Plate padding as a fraction of a cell, so the outer arrows never sit under
 * the frame and the dot field has room to run around the grid.
 */
private const val PlateInset = 0.30f

/**
 * How far a cell may stretch from square before the grid gives up filling the
 * plate and locks to the level's own ratio instead. The play area is portrait
 * on every supported device and the shipped levels are authored to the same
 * band, so in practice the grid fills the plate outright; these bounds only
 * keep an unusual window from stretching the maze. Arrows size their tube and
 * arrowhead off the smaller cell edge, so a cell inside this band only makes
 * the runs between bends longer, never fatter.
 */
private const val MinCellAspect = 0.78f
private const val MaxCellAspect = 1.26f

/** Ceiling on the extra dot lanes drawn past the grid, so a bad box cannot spin. */
private const val MaxDotLanes = 64

/** The seven neon arrow colours of the maze. */
internal val NeonPalette = listOf(
    NeonCyan,
    NeonMagenta,
    NeonViolet,
    NeonGreen,
    NeonOrange,
    NeonAmber,
    NeonBlue
)

/** A blocked arrow: which one, where it is stopped, and a repeat counter. */
data class ArrowBlock(val tileIndex: Int, val cell: BoardPosition, val count: Int)

/** A departure: which arrow is flying out, and a repeat counter for the trigger. */
data class ArrowLaunch(val tileIndex: Int, val trigger: Long)

/** What kind of assist treatment, if any, is drawn on the highlighted arrow. */
enum class TileHighlight { NONE, TUTORIAL, HINT }

/** Liquid flow profile: the front walks the body at an even rate, then leaves. */
private const val PipeDrainMillis = 400
private const val PipeExitMillis = 220
private const val BlockMillis = 460

/**
 * The neon arrow maze.
 *
 * The board draws into whatever box [modifier] hands it and derives its cell
 * geometry from the measured size, so callers own the sizing: fit the board to
 * the space available, and the arrows and the touch hit regions follow. The
 * grid fills that box on both axes — that is what the shipped portrait levels
 * are sized for — and only falls back to the level's own column:row ratio on a
 * window far off that shape, so cells are never stretched.
 *
 * Everything is painted in one canvas on a dark plate: the background matrix
 * dot field across the whole plate, the multi-cell arrows as glowing tubes, the
 * markers of cells an arrow has already vacated, the launch trail and exhaust,
 * and the red collision burst. Input is handled by hit testing against each
 * arrow's drawn spine, so tapping any part of a bent arrow selects that whole
 * arrow, and TalkBack gets one button node per arrow.
 *
 * [launchStyle] picks the departure animation. The board only asks for it, via
 * [LaunchAnimation.playTapEffect]; every curve, colour and size of the effect
 * itself lives outside this file, and nothing here reads the style for anything
 * but that one call.
 */
@Composable
fun ArrowBoard(
    state: PuzzleState,
    modifier: Modifier = Modifier,
    inputEnabled: Boolean = true,
    canSelect: (Int) -> Boolean = { true },
    highlightTileIndex: Int? = null,
    highlight: TileHighlight = TileHighlight.NONE,
    block: ArrowBlock? = null,
    launchArrow: ArrowLaunch? = null,
    launchStyle: LaunchAnimationStyle = LaunchAnimationStyle.Default,
    onLaunchFinished: (Int) -> Unit = {},
    onArrowTap: (Int) -> Unit
) {
    val level = state.level

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val metrics = remember(maxWidth, maxHeight, density, level) {
            BoardMetrics(
                widthPx = with(density) { maxWidth.toPx() },
                heightPx = with(density) { maxHeight.toPx() },
                cellGap = with(density) { CellGap.toPx() },
                columns = level.width,
                rows = level.height
            )
        }
        val shapes = remember(level, metrics) {
            level.tiles.map {
                ArrowShape(
                    tile = it,
                    cellWidth = metrics.cellWidth,
                    cellHeight = metrics.cellHeight,
                    gap = metrics.cellGap,
                    left = metrics.gridLeft,
                    top = metrics.gridTop
                )
            }
        }
        // The flattened spine of every arrow, built once instead of once per
        // frame: launch effects ride these curves and stay allocation free.
        val curves = remember(level, shapes, metrics) {
            shapes.map { CurvePath.of(it.spine, metrics.unit) }
        }
        val colors = remember(level) { assignArrowColors(level) }

        // The departure animation, owned by [LaunchAnimation]: the board triggers
        // it and draws whatever it reports, and holds no effect code of its own.
        val launchAnimation = rememberLaunchAnimation(launchStyle, level.id)

        val entrances = remember(level.id) { List(level.tiles.size) { Animatable(0f) } }
        LaunchedEffect(level.id) {
            entrances.forEachIndexed { index, anim ->
                launch {
                    if (index > 0) delay((index * 20L).coerceAtMost(700L))
                    anim.animateTo(1f, tween(260))
                }
            }
        }

        // Liquid-flow departure, in two beats.
        //
        // The first clock is the drain: the flow front walks the arrow's own bent
        // path from tail to tip at an even rate, the head riding it and the tube
        // behind it collapsing to a line. Linear, because a pipe flow is a
        // constant-velocity thing and the front has to cross every bend at the
        // same visible speed.
        //
        // The second is the exit, which only starts once the drain is over and
        // the whole arrow is a thin line: that line is then carried out of the
        // board along the arrow's own exit axis. It hands over at the drain's
        // speed, so there is no stall between the two.
        //
        // Each launch animates in its own coroutine so two quick taps cannot
        // cancel each other.
        val drains = remember(level.id) { mutableStateMapOf<Long, Animatable<Float, *>>() }
        val exits = remember(level.id) { mutableStateMapOf<Long, Animatable<Float, *>>() }
        val launchTiles = remember(level.id) { mutableStateMapOf<Long, Int>() }
        val flightScope = rememberCoroutineScope()
        val launchFinished = rememberUpdatedState(onLaunchFinished)
        LaunchedEffect(launchArrow?.trigger, level.id) {
            val spec = launchArrow ?: return@LaunchedEffect
            // The tap effect fires on the same signal and the same frame as the
            // departure, so the pulse is already open under the arrow as it goes.
            val tapped = shapes.getOrNull(spec.tileIndex)
            if (tapped != null) {
                launchAnimation.playTapEffect(
                    arrow = tapped,
                    color = colors.getOrElse(spec.tileIndex) { NeonCyan },
                    tileIndex = spec.tileIndex
                )
            }
            val drainAnim = Animatable(0f)
            val exitAnim = Animatable(0f)
            launchTiles[spec.trigger] = spec.tileIndex
            drains[spec.trigger] = drainAnim
            exits[spec.trigger] = exitAnim
            flightScope.launch {
                drainAnim.animateTo(1f, tween(PipeDrainMillis, easing = LinearEasing))
                exitAnim.animateTo(
                    1f,
                    tween(PipeExitMillis, easing = LinearOutSlowInEasing)
                )
                drains.remove(spec.trigger)
                exits.remove(spec.trigger)
                launchTiles.remove(spec.trigger)
                // The arrow is now fully off the board. The clear celebration
                // waits on this so it never starts while the final arrow is still
                // leaving.
                launchFinished.value(spec.tileIndex)
            }
        }

        // Blocked feedback: one animation drives the shake, the red flash and
        // the collision burst so they always stay in sync.
        val blockAnim = remember(level.id) { Animatable(1f) }
        LaunchedEffect(block?.count, level.id) {
            if (block == null) {
                blockAnim.snapTo(1f)
            } else {
                blockAnim.snapTo(0f)
                blockAnim.animateTo(1f, tween(BlockMillis))
            }
        }

        // Read inside the draw block only, so neither restarts recomposition.
        val breath = rememberInfiniteTransition(label = "breath").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
        val flicker = rememberInfiniteTransition(label = "flicker").animateFloat(
            initialValue = 0.86f,
            targetValue = 1.16f,
            animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
            label = "flicker"
        )

        val highlightColor = when (highlight) {
            TileHighlight.HINT -> NeonAmber
            TileHighlight.TUTORIAL -> NeonCyan
            TileHighlight.NONE -> null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(level.id, inputEnabled, metrics, shapes, state.remainingTiles) {
                    detectTapGestures { tap ->
                        if (!inputEnabled) return@detectTapGestures
                        val hit = metrics.hitTile(shapes, level, state.remainingTiles, tap)
                        if (hit != null && canSelect(hit)) onArrowTap(hit)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val blockProgress = blockAnim.value.coerceIn(0f, 1f)
                val flyingTiles = launchTiles.values.toSet()
                drawPlate(metrics, state, flyingTiles)

                // Tap pulses sit on the plate, under every arrow, so a pulse marks
                // the spot an arrow left without washing out the one that is still
                // there next to it.
                launchAnimation.draw(this)

                for (index in level.tiles.indices) {
                    val tile = level.tiles[index]
                    if (tile.cells.first() !in state.remainingTiles) continue

                    var alpha = entrances[index].value.coerceIn(0f, 1f)
                    var glow = 0f
                    var scale = 1f
                    var color = colors[index]
                    var translation = Offset.Zero

                    // A hint or tutorial marker is a repaint first: the arrow takes
                    // the accent tint so it separates from its neighbours at a
                    // glance, and only then pulses. The glow stays inside the band
                    // ArrowShape maps to a visible energy ramp, since a larger value
                    // would sit past its ceiling and read as a flat, dead glow.
                    val highlightTint = highlightColor
                    if (index == highlightTileIndex && highlightTint != null) {
                        val wave = breath.value
                        color = highlightTint
                        glow += 0.08f + 0.38f * wave
                        scale += 0.05f * wave
                        if (alpha > 0.01f) {
                            drawHighlightRing(
                                center = metrics.center(tile.position),
                                unit = metrics.unit,
                                tint = highlightTint,
                                wave = wave,
                                opacity = alpha
                            )
                        }
                    }

                    if (index == block?.tileIndex && blockProgress < 1f) {
                        color = NeonRed
                        glow += 1.7f * (1f - blockProgress)
                        alpha *= 1f - 0.55f * smoothStep(0.6f, 1f, blockProgress)
                        translation = Offset(
                            x = shakeOffset(blockProgress, metrics.unit),
                            y = 0f
                        )
                    }

                    if (alpha > 0.01f) {
                        shapes[index].draw(
                            scope = this,
                            color = color,
                            glow = glow,
                            translation = translation,
                            scale = scale,
                            alpha = alpha
                        )
                    }
                }

                if (block != null && blockProgress < 1f) {
                    drawCollision(metrics.center(block.cell), metrics.unit, blockProgress)
                }

                for (entry in drains) {
                    val flyingIndex = launchTiles[entry.key] ?: continue
                    val tile = level.tiles.getOrNull(flyingIndex) ?: continue
                    val shape = shapes.getOrNull(flyingIndex) ?: continue
                    val curve = curves.getOrNull(flyingIndex) ?: continue
                    val drain = entry.value.value.coerceIn(0f, 1f)
                    val exit = exits[entry.key]?.value?.coerceIn(0f, 1f) ?: 0f
                    if (exit >= 1f) continue
                    drawLaunch(
                        shape = shape,
                        curve = curve,
                        color = colors.getOrElse(flyingIndex) { NeonCyan },
                        direction = tile.direction,
                        metrics = metrics,
                        drain = drain,
                        exit = exit,
                        flicker = flicker.value,
                        boost = launchAnimation.arrowGlowBoost(flyingIndex)
                    )
                }
            }
        }

        // One TalkBack button per remaining arrow, anchored on its head cell.
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(level.height) { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(level.width) { column ->
                        val position = BoardPosition(row, column)
                        val tileIndex = level.occupancy[position]
                        val tile = tileIndex?.let { level.tiles[it] }
                        val remaining = tile != null && tile.cells.first() in state.remainingTiles
                        val selectable = remaining &&
                            inputEnabled &&
                            state.status == GameStatus.PLAYING &&
                            canSelect(tileIndex!!)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (selectable) {
                                        Modifier.semantics {
                                            role = Role.Button
                                            contentDescription =
                                                "Arrow ${tileIndex + 1} of ${level.tiles.size}, " +
                                                    "pointing ${tile!!.direction.name.lowercase()}, " +
                                                    "${tile.cells.size} cells long, row ${row + 1}, " +
                                                    "column ${column + 1}"
                                            onClick(label = "Remove arrow") { onArrowTap(tileIndex); true }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cell geometry for the current board size.
 *
 * The grid fills the plate on both axes, so the maze uses the whole play area
 * instead of floating as a letterboxed square inside it. Cells only come out
 * square because the shipped levels are authored to the same portrait ratio as
 * the play area; if a window's aspect ever drifts further than
 * [MinCellAspect]..[MaxCellAspect], the grid falls back to locking the level's
 * own column:row ratio and centres itself, so arrows are never stretched.
 *
 * Whatever the fit, the leftover margin on the locked axis stays plate and is
 * covered by the dot field, and [latticeColumns]/[latticeRows] report the lanes
 * that reach into it.
 */
internal class BoardMetrics(
    private val widthPx: Float,
    private val heightPx: Float,
    val cellGap: Float,
    val columns: Int,
    val rows: Int
) {
    private val gridWidth: Float
    private val gridHeight: Float

    /** Distance from the plate edge to the grid on each axis. */
    val gridLeft: Float
    val gridTop: Float

    init {
        val aspect = columns / rows.toFloat()
        // Probe the locked fit first: it yields the cell size, which sets the
        // plate padding, which the real fit is then measured inside. A fixed dp
        // value would read tight beside a 4x7 tutorial grid and lost beside a
        // 7x11 pack-5 board.
        val inset = squareCellSize(widthPx, heightPx, aspect) * PlateInset
        val availWidth = max(0f, widthPx - inset * 2f)
        val availHeight = max(0f, heightPx - inset * 2f)
        val stretchedWidth = availWidth / columns
        val stretchedHeight = availHeight / rows
        val cellAspect = if (stretchedHeight > 0f) stretchedWidth / stretchedHeight else 0f

        if (cellAspect in MinCellAspect..MaxCellAspect) {
            gridWidth = availWidth
            gridHeight = availHeight
            gridLeft = inset
            gridTop = inset
        } else {
            // Far off-ratio box: keep square cells and take the margin as plate.
            val locked = lockedFit(availWidth, availHeight, aspect)
            gridWidth = locked.first
            gridHeight = locked.second
            gridLeft = inset + (availWidth - gridWidth) / 2f
            gridTop = inset + (availHeight - gridHeight) / 2f
        }
    }

    /** The largest level-ratio rectangle that fits the box, as width to height. */
    private fun lockedFit(availWidth: Float, availHeight: Float, aspect: Float): Pair<Float, Float> {
        if (availWidth <= 0f || availHeight <= 0f || aspect <= 0f) return 0f to 0f
        return if (availWidth / availHeight > aspect) {
            (availHeight * aspect) to availHeight
        } else {
            availWidth to (availWidth / aspect)
        }
    }

    /** The cell edge a locked fit of [aspect] gives inside this box. */
    private fun squareCellSize(boxWidth: Float, boxHeight: Float, aspect: Float): Float {
        val grid = lockedFit(boxWidth, boxHeight, aspect)
        if (grid.first <= 0f || grid.second <= 0f) return 0f
        return min(
            (grid.first - cellGap * (columns - 1)) / columns,
            (grid.second - cellGap * (rows - 1)) / rows
        )
    }

    val cellWidth: Float = (gridWidth - cellGap * (columns - 1)) / columns
    val cellHeight: Float = (gridHeight - cellGap * (rows - 1)) / rows
    val unit: Float = min(cellWidth, cellHeight)

    /** The full plate, which the dot field covers edge to edge. */
    val plateWidth: Float get() = widthPx
    val plateHeight: Float get() = heightPx

    /** Distance between neighbouring cell centres: the dot matrix runs on it. */
    private val pitchX: Float = cellWidth + cellGap
    private val pitchY: Float = cellHeight + cellGap
    private val originX: Float = gridLeft + cellWidth / 2f
    private val originY: Float = gridTop + cellHeight / 2f

    fun center(position: BoardPosition): Offset = Offset(
        x = originX + position.column * pitchX,
        y = originY + position.row * pitchY
    )

    /**
     * The centre of any dot lane, including the lanes past the level's own
     * grid, so the background matrix can cover the margin the grid leaves.
     */
    fun latticeCenter(column: Int, row: Int): Offset = Offset(
        x = originX + column * pitchX,
        y = originY + row * pitchY
    )

    /** Dot lanes whose centres still land on the plate. */
    fun latticeColumns(margin: Float): IntRange = laneRange(originX, pitchX, margin, widthPx)

    fun latticeRows(margin: Float): IntRange = laneRange(originY, pitchY, margin, heightPx)

    private fun laneRange(origin: Float, pitch: Float, margin: Float, extent: Float): IntRange {
        if (pitch <= 0f || extent <= 0f) return 0..0
        val first = floor((-margin - origin) / pitch).toInt().coerceAtLeast(-MaxDotLanes)
        val last = floor((extent + margin - origin) / pitch).toInt().coerceAtMost(MaxDotLanes)
        return if (last < first) 0..0 else first..last
    }

    private fun positionAt(x: Float, y: Float): BoardPosition? {
        val localX = x - gridLeft
        val localY = y - gridTop
        if (localX < 0f || localY < 0f || localX > gridWidth || localY > gridHeight) return null
        val column = ((localX - cellWidth / 2f) / pitchX).toInt()
        val row = ((localY - cellHeight / 2f) / pitchY).toInt()
        return if (row in 0 until rows && column in 0 until columns) {
            BoardPosition(row, column)
        } else {
            null
        }
    }

    /**
     * The arrow whose drawn body is closest to [tap]. Falls back to the arrow
     * that owns the tapped cell so a near-miss still selects something.
     */
    fun hitTile(
        shapes: List<ArrowShape>,
        level: PuzzleLevel,
        remaining: Set<BoardPosition>,
        tap: Offset
    ): Int? {
        var bestIndex: Int? = null
        var bestDistance = Float.MAX_VALUE
        for (index in shapes.indices) {
            if (level.tiles[index].cells.first() !in remaining) continue
            val distance = shapes[index].distanceTo(tap.x, tap.y)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        val radius = shapes.firstOrNull()?.hitRadius ?: return null
        if (bestIndex != null && bestDistance <= radius) return bestIndex
        val cell = positionAt(tap.x, tap.y) ?: return null
        return level.ownerAt(cell)?.takeIf { level.tiles[it].cells.first() in remaining }
    }

    /** Distance an arrow tip at [tip] must travel to clear the plate. */
    fun travelFor(direction: Direction, tip: Offset): Float = when (direction) {
        Direction.RIGHT -> widthPx - tip.x + unit * 1.4f
        Direction.LEFT -> tip.x + unit * 1.4f
        Direction.DOWN -> heightPx - tip.y + unit * 1.4f
        Direction.UP -> tip.y + unit * 1.4f
    }
}

/**
 * Assigns arrow colours so no two touching arrows share one. Placement order
 * drives the choice and only earlier neighbours are considered, so each arrow
 * avoids the colours already around it — the reason a dense maze stays legible.
 */
internal fun assignArrowColors(level: PuzzleLevel): List<Color> {
    val assigned = ArrayList<Color>(level.tiles.size)
    for (index in level.tiles.indices) {
        val forbidden = HashSet<Color>(NeonPalette.size)
        for (cell in level.tiles[index].cells) {
            for (neighbor in cell.neighbors()) {
                val owner = level.occupancy[neighbor]
                if (owner != null && owner < index) forbidden += assigned[owner]
            }
        }
        assigned += NeonPalette.firstOrNull { it !in forbidden }
            ?: NeonPalette[index % NeonPalette.size]
    }
    return assigned
}

private fun BoardPosition.neighbors(): List<BoardPosition> = listOf(
    BoardPosition(row - 1, column),
    BoardPosition(row + 1, column),
    BoardPosition(row, column - 1),
    BoardPosition(row, column + 1)
)

/**
 * The dark plate the maze sits on, over the background matrix dot field.
 *
 * The field runs on the grid's own cell pitch across the whole plate, not just
 * under the cells the level happens to use, so any plate the grid leaves over —
 * the inset frame, or the margin on a window whose aspect drifts off the
 * level's — reads as part of the board instead of bare background.
 */
private fun DrawScope.drawPlate(
    metrics: BoardMetrics,
    state: PuzzleState,
    flyingTiles: Set<Int>
) {
    val radius = CornerRadius(metrics.unit * 0.30f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(BoardPlate, StageDeep, StageVoid)),
        cornerRadius = radius
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to NeonCyan.copy(alpha = 0.10f),
            0.45f to NeonViolet.copy(alpha = 0.05f),
            1f to Color.Transparent
        ),
        cornerRadius = radius
    )
    drawRoundRect(
        color = BoardFrame.copy(alpha = 0.65f),
        cornerRadius = radius,
        style = Stroke(width = metrics.unit * 0.045f)
    )
    drawRoundRect(
        color = BoardPlateEdge.copy(alpha = 0.8f),
        cornerRadius = radius,
        style = Stroke(width = metrics.unit * 0.02f)
    )

    val flyingCells = flyingTiles
        .mapNotNull { state.level.tiles.getOrNull(it) }
        .flatMap { it.cells }
        .toSet()
    val fade = max(metrics.unit * 0.30f, 1f)
    val dotRadius = metrics.unit * 0.035f
    for (row in metrics.latticeRows(fade)) {
        for (column in metrics.latticeColumns(fade)) {
            val center = metrics.latticeCenter(column, row)
            if (row !in 0 until metrics.rows || column !in 0 until metrics.columns) {
                // A lane past the grid: the expanded plate still reads as a
                // matrix, dimmed and dissolved into the frame at the edges.
                drawCircle(
                    color = GridDotDim.copy(alpha = 0.42f * plateEdgeFade(metrics, center, fade)),
                    radius = dotRadius * 0.82f,
                    center = center
                )
                continue
            }
            val position = BoardPosition(row, column)
            drawCircle(
                color = NeonTextDim.copy(alpha = 0.10f),
                radius = dotRadius,
                center = center
            )
            if (position in state.remainingTiles) continue
            // Vacated cells keep a brighter marker so the grid shows through as
            // the maze unwinds. The arrow still in flight has not vacated yet.
            if (position in flyingCells) continue
            drawCircle(
                color = NeonTextDim.copy(alpha = 0.45f),
                radius = metrics.unit * 0.045f,
                center = center
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.22f),
                radius = metrics.unit * 0.11f,
                center = center,
                style = Stroke(width = metrics.unit * 0.022f)
            )
        }
    }
}

/**
 * Fades a dot out as it nears the plate border, so the matrix dissolves into
 * the frame instead of stopping on a hard rectangular line.
 */
private fun plateEdgeFade(metrics: BoardMetrics, center: Offset, margin: Float): Float {
    val x = min(center.x, metrics.plateWidth - center.x)
    val y = min(center.y, metrics.plateHeight - center.y)
    return (min(x, y) / margin).coerceIn(0f, 1f)
}

/**
 * A sampled version of one arrow's body curve, with cumulative arc length so
 * effects can be positioned by distance along the real path instead of by
 * straight lines from tail to head.
 *
 * [corners] holds the arc length of the closest point of the drawn curve to each
 * real bend, so an effect can light a corner at the moment its travelling front
 * actually gets there.
 */
internal class CurvePath(
    val points: List<Offset>,
    val cumulative: FloatArray,
    val corners: FloatArray = FloatArray(0)
) {

    val length: Float get() = cumulative[cumulative.size - 1]

    fun pointAt(distance: Float): Offset {
        val d = distance.coerceIn(0f, length)
        for (index in 1 until cumulative.size) {
            if (d <= cumulative[index]) {
                val span = cumulative[index] - cumulative[index - 1]
                val t = if (span < 0.0001f) 0f else (d - cumulative[index - 1]) / span
                return Offset(
                    x = points[index - 1].x + (points[index].x - points[index - 1].x) * t,
                    y = points[index - 1].y + (points[index].y - points[index - 1].y) * t
                )
            }
        }
        return points.last()
    }

    /** The part of the curve between two distances, ends interpolated. */
    fun slice(from: Float, to: Float): List<Offset> {
        if (to <= from) return emptyList()
        val out = ArrayList<Offset>(points.size + 2)
        out += pointAt(from)
        for (index in 1 until points.size) {
            if (cumulative[index] > from && cumulative[index] < to) out += points[index]
        }
        out += pointAt(to)
        return out
    }

    fun pathOf(segment: List<Offset>): Path {
        val path = Path()
        if (segment.isEmpty()) return path
        path.moveTo(segment.first().x, segment.first().y)
        for (index in 1 until segment.size) path.lineTo(segment[index].x, segment[index].y)
        return path
    }

    /**
     * Rewrites [path] to hold only the stretch of the curve between two arc
     * lengths, reusing the caller's [Path] so a launch frame that walks the tube
     * in several pieces does not allocate one per piece.
     */
    fun fillPath(path: Path, from: Float, to: Float) {
        val start = from.coerceAtLeast(0f)
        val end = min(to, length)
        path.reset()
        if (end <= start) return
        val head = pointAt(start)
        path.moveTo(head.x, head.y)
        for (index in 1 until points.size) {
            val distance = cumulative[index]
            if (distance > start && distance < end) {
                path.lineTo(points[index].x, points[index].y)
            }
        }
        val tail = pointAt(end)
        path.lineTo(tail.x, tail.y)
    }

    /**
     * Unit vector of the curve at [distance], sampled either side of it. Used to
     * aim an exhaust or a perpendicular spark offset, so both follow a bent
     * arrow instead of a board axis.
     */
    fun directionAt(distance: Float): Offset {
        val step = maxOf(length * 0.03f, 1f)
        val delta = pointAt(distance + step) - pointAt(distance - step)
        val size = delta.getDistance()
        return if (size < 0.0001f) Offset(1f, 0f) else delta * (1f / size)
    }

    /** Unit vector across the curve at [distance]. */
    fun normalAt(distance: Float): Offset {
        val along = directionAt(distance)
        return Offset(x = -along.y, y = along.x)
    }

    companion object {
        /**
         * Flattens an arrow's spine into a dense polyline that follows the same
         * quadratic Bezier bends `ArrowShape` draws, so a travelling highlight
         * rides the real curve and wraps around L, U and zigzag corners instead
         * of cutting across them. The bend rule mirrors `smoothPolyline`.
         */
        fun of(spine: List<Offset>, unit: Float): CurvePath {
            if (spine.size < 2) {
                val flat = if (spine.isEmpty()) listOf(Offset.Zero) else spine
                return CurvePath(flat, FloatArray(flat.size))
            }

            val radius = unit * 0.42f
            val samples = 6
            val out = ArrayList<Offset>(spine.size * (samples + 2))
            val bendSamples = ArrayList<Int>(spine.size)
            out += spine.first()
            for (index in 1 until spine.size - 1) {
                val corner = spine[index]
                val inX = corner.x - spine[index - 1].x
                val inY = corner.y - spine[index - 1].y
                val outX = spine[index + 1].x - corner.x
                val outY = spine[index + 1].y - corner.y
                val inLen = kotlin.math.sqrt(inX * inX + inY * inY)
                val outLen = kotlin.math.sqrt(outX * outX + outY * outY)
                if (inLen < 0.01f || outLen < 0.01f) continue
                val inDirX = inX / inLen
                val inDirY = inY / inLen
                val outDirX = outX / outLen
                val outDirY = outY / outLen
                val cross = inDirX * outDirY - inDirY * outDirX
                val dot = inDirX * outDirX + inDirY * outDirY
                if (abs(cross) < 0.01f && dot > 0f) {
                    out += corner
                    continue
                }
                val r = minOf(radius, minOf(inLen * 0.42f, outLen * 0.42f))
                val beforeX = corner.x - inDirX * r
                val beforeY = corner.y - inDirY * r
                val afterX = corner.x + outDirX * r
                val afterY = corner.y + outDirY * r
                for (step in 0..samples) {
                    val t = step / samples.toFloat()
                    val u = 1f - t
                    out += Offset(
                        x = beforeX * u * u + corner.x * 2f * u * t + afterX * t * t,
                        y = beforeY * u * u + corner.y * 2f * u * t + afterY * t * t
                    )
                    // The mid sample is the closest the drawn curve gets to the corner.
                    if (step * 2 == samples) bendSamples += out.size - 1
                }
            }
            out += spine.last()

            val cumulative = FloatArray(out.size)
            for (index in 1 until out.size) {
                cumulative[index] = cumulative[index - 1] + (out[index] - out[index - 1]).getDistance()
            }
            val corners = FloatArray(bendSamples.size) { cumulative[bendSamples[it]] }
            return CurvePath(out, cumulative, corners)
        }
    }
}

/**
 * The pulsing ring that marks the arrow a hint or tutorial points at.
 *
 * Drawn under the arrow itself: the ring is the beacon, and the tinted arrow
 * riding on top of it is what it is pointing at.
 */
private fun DrawScope.drawHighlightRing(
    center: Offset,
    unit: Float,
    tint: Color,
    wave: Float,
    opacity: Float
) {
    val pulse = 0.5f + 0.5f * sin(wave * Math.PI.toFloat())
    drawCircle(
        color = tint.copy(alpha = 0.16f * opacity),
        radius = unit * (0.52f + 0.10f * pulse),
        center = center
    )
    drawCircle(
        color = NeonCore.copy(alpha = (0.34f + 0.30f * pulse) * opacity),
        radius = unit * (0.40f + 0.13f * pulse),
        center = center,
        style = Stroke(width = unit * (0.020f + 0.012f * pulse))
    )
    drawCircle(
        color = tint.copy(alpha = (0.44f + 0.26f * pulse) * opacity),
        radius = unit * (0.40f + 0.13f * pulse),
        center = center,
        style = Stroke(width = unit * 0.012f)
    )
}

/** Red flash, expanding ring, and radiating sparks where a blocked arrow stops. */
private fun DrawScope.drawCollision(center: Offset, unit: Float, progress: Float) {
    if (progress >= 1f) return
    val fade = 1f - progress
    drawCircle(
        color = NeonRed.copy(alpha = 0.32f * fade * fade),
        radius = unit * (0.18f + 0.6f * progress),
        center = center
    )
    drawCircle(
        color = NeonRed.copy(alpha = 0.9f * fade),
        radius = unit * (0.24f + 0.66f * progress),
        center = center,
        style = Stroke(width = unit * (0.02f + 0.05f * fade))
    )
    drawCircle(
        color = NeonCore.copy(alpha = 0.7f * fade),
        radius = unit * 0.13f * fade,
        center = center
    )
    val reach = unit * (0.25f + 0.85f * progress)
    for (spark in 0 until 8) {
        val angle = spark * (Math.PI / 4.0).toFloat()
        val dx = cos(angle)
        val dy = sin(angle)
        drawLine(
            color = NeonRed.copy(alpha = 0.85f * fade),
            start = Offset(center.x + dx * reach * 0.55f, center.y + dy * reach * 0.55f),
            end = Offset(center.x + dx * reach, center.y + dy * reach),
            strokeWidth = unit * 0.035f * fade,
            cap = StrokeCap.Round
        )
    }
}

private fun shakeOffset(progress: Float, unit: Float): Float {
    if (progress <= 0f || progress >= 1f) return 0f
    val decay = 1f - progress
    return sin(progress * Math.PI.toFloat() * 7f) * decay * unit * 0.17f
}

private fun smoothStep(from: Float, to: Float, value: Float): Float {
    if (to - from < 0.0001f) return if (value >= to) 1f else 0f
    return ((value - from) / (to - from)).coerceIn(0f, 1f)
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

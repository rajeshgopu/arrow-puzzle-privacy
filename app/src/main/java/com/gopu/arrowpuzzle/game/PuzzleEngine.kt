package com.gopu.arrowpuzzle.game

enum class Direction(val rowDelta: Int, val columnDelta: Int) {
    UP(-1, 0),
    RIGHT(0, 1),
    DOWN(1, 0),
    LEFT(0, -1)
}

data class BoardPosition(val row: Int, val column: Int)

/**
 * An arrow piece. [cells] is the ordered body from tail to head; the head
 * (last cell) carries the arrowhead and the exit [direction]. A single-cell
 * arrow is a straight arrow; a multi-cell arrow is a bent arrow whose path
 * turns through its body cells.
 */
data class ArrowTile(
    val cells: List<BoardPosition>,
    val direction: Direction
) {
    init {
        require(cells.isNotEmpty()) { "An arrow needs at least one cell." }
    }

    /** The head cell — where the arrow leaves and where the arrowhead is drawn. */
    val position: BoardPosition get() = cells.last()

    constructor(position: BoardPosition, direction: Direction) : this(listOf(position), direction)
}

data class PuzzleLevel(
    val id: String,
    val pack: Int,
    val order: Int,
    val width: Int,
    val height: Int,
    val tiles: List<ArrowTile>,
    val parMoves: Int = 0
) {
    /**
     * Exclusive cell ownership: every occupied cell maps to exactly one tile
     * index. Treating the board as a matrix guarantees no two arrows ever share
     * a cell, which is what keeps bent arrows from overlapping on screen.
     */
    val occupancy: Map<BoardPosition, Int> by lazy {
        HashMap<BoardPosition, Int>(tiles.sumOf { it.cells.size }).apply {
            tiles.forEachIndexed { index, tile -> tile.cells.forEach { put(it, index) } }
        }
    }

    init {
        require(width > 0 && height > 0) { "Board dimensions must be positive." }
        val occupied = tiles.flatMap { it.cells }
        require(occupied.distinct().size == occupied.size) {
            "A level cannot contain overlapping tiles."
        }
        require(occupied.all { it.isInside(this) }) {
            "Every tile must be positioned inside its board."
        }
    }

    fun tileAt(position: BoardPosition): ArrowTile? =
        tiles.firstOrNull { position in it.cells }

    /** The index of the tile owning [position], or `null` when the cell is empty. */
    fun ownerAt(position: BoardPosition): Int? = occupancy[position]
}

enum class GameStatus { PLAYING, WON, LOST }

data class PuzzleState(
    val level: PuzzleLevel,
    val remainingTiles: Set<BoardPosition> = level.tiles.flatMap { it.cells }.toSet(),
    val lives: Int = 3,
    val invalidTaps: Int = 0,
    val status: GameStatus = GameStatus.PLAYING
)

object PuzzleReducer {
    fun start(level: PuzzleLevel): PuzzleState = PuzzleState(level = level)

    fun restart(state: PuzzleState): PuzzleState = start(state.level)

    fun tap(state: PuzzleState, position: BoardPosition): PuzzleState {
        if (state.status != GameStatus.PLAYING || position !in state.remainingTiles) {
            return state
        }

        val tile = state.level.tileAt(position) ?: return state
        return if (isPathClear(state, tile)) {
            val remainingTiles = state.remainingTiles - tile.cells.toSet()
            state.copy(
                remainingTiles = remainingTiles,
                status = if (remainingTiles.isEmpty()) GameStatus.WON else GameStatus.PLAYING
            )
        } else {
            val lives = state.lives - 1
            state.copy(
                lives = lives,
                invalidTaps = state.invalidTaps + 1,
                status = if (lives == 0) GameStatus.LOST else GameStatus.PLAYING
            )
        }
    }

    fun isPathClear(state: PuzzleState, tile: ArrowTile): Boolean {
        val index = state.level.tiles.indexOf(tile)
        var position = tile.position.step(tile.direction)
        while (position.isInside(state.level)) {
            val owner = state.level.ownerAt(position)
            // The moving arrow's own body cells leave with it, so they never
            // block its exit; only cells owned by another remaining tile do.
            if (owner != null && owner != index && position in state.remainingTiles) {
                return false
            }
            position = position.step(tile.direction)
        }
        return true
    }

    fun firstValidMove(state: PuzzleState): BoardPosition? =
        state.level.tiles
            .map(ArrowTile::position)
            .firstOrNull { position ->
                position in state.remainingTiles &&
                    state.level.tileAt(position)?.let { isPathClear(state, it) } == true
            }
}

fun starsForInvalidTaps(invalidTaps: Int): Int = when {
    invalidTaps <= 0 -> 3
    invalidTaps == 1 -> 2
    else -> 1
}

private fun BoardPosition.step(direction: Direction): BoardPosition = BoardPosition(
    row = row + direction.rowDelta,
    column = column + direction.columnDelta
)

private fun BoardPosition.isInside(level: PuzzleLevel): Boolean =
    row in 0 until level.height && column in 0 until level.width

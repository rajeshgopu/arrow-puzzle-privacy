package com.gopu.arrowpuzzle.game

enum class Direction(val rowDelta: Int, val columnDelta: Int) {
    UP(-1, 0),
    RIGHT(0, 1),
    DOWN(1, 0),
    LEFT(0, -1)
}

data class BoardPosition(val row: Int, val column: Int)

data class ArrowTile(val position: BoardPosition, val direction: Direction)

data class PuzzleLevel(
    val id: String,
    val pack: Int,
    val order: Int,
    val width: Int,
    val height: Int,
    val tiles: List<ArrowTile>,
    val parMoves: Int = 0
) {
    init {
        require(width > 0 && height > 0) { "Board dimensions must be positive." }
        require(tiles.map(ArrowTile::position).distinct().size == tiles.size) {
            "A level cannot contain overlapping tiles."
        }
        require(tiles.all { it.position.isInside(this) }) {
            "Every tile must be positioned inside its board."
        }
    }

    fun tileAt(position: BoardPosition): ArrowTile? = tiles.firstOrNull { it.position == position }
}

enum class GameStatus { PLAYING, WON, LOST }

data class PuzzleState(
    val level: PuzzleLevel,
    val remainingTiles: Set<BoardPosition> = level.tiles.map(ArrowTile::position).toSet(),
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
            val remainingTiles = state.remainingTiles - position
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
        var position = tile.position.step(tile.direction)
        while (position.isInside(state.level)) {
            if (position in state.remainingTiles) return false
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

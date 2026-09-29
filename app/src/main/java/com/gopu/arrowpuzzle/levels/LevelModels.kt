package com.gopu.arrowpuzzle.levels

import com.google.gson.Gson
import com.gopu.arrowpuzzle.game.ArrowTile
import com.gopu.arrowpuzzle.game.BoardPosition
import com.gopu.arrowpuzzle.game.Direction
import com.gopu.arrowpuzzle.game.PuzzleLevel

/** Levels shipped per pack; used for global (cross-pack) progression math. */
const val LEVELS_PER_PACK = 10

data class LevelJson(
    val id: String,
    val pack: Int,
    val order: Int,
    val width: Int,
    val height: Int,
    val tiles: List<LevelTile>,
    val parMoves: Int
)

data class LevelTile(
    val row: Int,
    val column: Int,
    val direction: String,
    /**
     * Optional ordered body cells (tail..head) for a bent/multi-cell arrow.
     * When null or empty the tile is a single-cell straight arrow at [row],[column].
     */
    val cells: List<LevelCell>? = null
)

data class LevelCell(val row: Int, val column: Int)

fun parseLevelJson(json: String): LevelJson {
    return Gson().fromJson(json, LevelJson::class.java)
}

/** Effective occupied cells of a tile, whether bent or straight. */
fun LevelTile.occupiedCells(): List<Pair<Int, Int>> =
    cells?.takeIf { it.isNotEmpty() }?.map { it.row to it.column } ?: listOf(row to column)

fun validateLevel(level: LevelJson): Boolean {
    if (level.width <= 0 || level.height <= 0) return false
    if (level.tiles.isEmpty()) return false

    val allCells = level.tiles.flatMap { it.occupiedCells() }
    if (allCells.isEmpty()) return false
    if (allCells.size > level.width * level.height) return false
    if (allCells.distinct().size != allCells.size) return false
    if (!allCells.all { it.first in 0 until level.height && it.second in 0 until level.width }) return false

    val validDirections = setOf("UP", "RIGHT", "DOWN", "LEFT")
    if (!level.tiles.all { it.direction in validDirections }) return false

    // A bent arrow's body must be contiguous (each cell adjacent to the next).
    if (!level.tiles.all { tile -> tile.occupiedCells().zipWithNext().all { (a, b) -> manhattan(a, b) == 1 } }) {
        return false
    }

    // A bent arrow's exit direction must match the direction of its final body
    // segment into the head, otherwise the arrowhead would point away from its
    // own body (the "unclear bend" the renderer used to draw).
    if (!level.tiles.all { tile -> exitMatchesBody(tile) }) return false

    return true
}

private fun exitMatchesBody(tile: LevelTile): Boolean {
    val cells = tile.occupiedCells()
    if (cells.size < 2) return true
    val (prevRow, prevColumn) = cells[cells.size - 2]
    val (headRow, headColumn) = cells.last()
    val segment = when {
        headRow < prevRow -> "UP"
        headRow > prevRow -> "DOWN"
        headColumn < prevColumn -> "LEFT"
        headColumn > prevColumn -> "RIGHT"
        else -> ""
    }
    return segment == tile.direction
}

fun LevelJson.toJson(): String = Gson().toJson(this)

fun PuzzleLevel.toLevelJson(): LevelJson = LevelJson(
    id = id,
    pack = pack,
    order = order,
    width = width,
    height = height,
    tiles = tiles.map { tile ->
        if (tile.cells.size <= 1) {
            LevelTile(tile.position.row, tile.position.column, tile.direction.name)
        } else {
            LevelTile(
                row = tile.position.row,
                column = tile.position.column,
                direction = tile.direction.name,
                cells = tile.cells.map { LevelCell(it.row, it.column) }
            )
        }
    },
    parMoves = parMoves
)

private fun manhattan(a: Pair<Int, Int>, b: Pair<Int, Int>): Int =
    kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)

fun LevelJson.toPuzzleLevel(): PuzzleLevel = PuzzleLevel(
    id = id,
    pack = pack,
    order = order,
    width = width,
    height = height,
    tiles = tiles.map { tile ->
        val body = tile.cells
        if (body != null && body.isNotEmpty()) {
            ArrowTile(
                cells = body.map { BoardPosition(it.row, it.column) },
                direction = Direction.valueOf(tile.direction)
            )
        } else {
            ArrowTile(
                BoardPosition(tile.row, tile.column),
                Direction.valueOf(tile.direction)
            )
        }
    },
    parMoves = parMoves
)

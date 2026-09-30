package com.gopu.arrow.puzzle.game

import kotlin.random.Random

/** Parameters for [PuzzleGenerator]. */
data class PuzzleGeneratorConfig(
    val width: Int,
    val height: Int,
    val arrowCount: Int,
    /** Longest body an arrow may have. `1` yields straight single-cell arrows. */
    val maxBodyCells: Int = 1,
    val id: String = "generated",
    val pack: Int = 0,
    val order: Int = 0
)

/**
 * Generates solvable levels by reverse construction.
 *
 * Arrows are placed in the reverse of a valid removal order: each newly placed
 * arrow only has to keep its exit ray clear of the arrows placed before it,
 * because the arrows placed after it are removed first and are gone by the time
 * this arrow leaves. That makes every generated level solvable by the same
 * greedy rule the game engine uses, with no search needed.
 *
 * Cells are owned through a matrix (`owner[row][column]`), so two arrows can
 * never share a cell, and a bent arrow's exit direction always matches the
 * direction of the final body segment into its head.
 */
object PuzzleGenerator {

    fun generate(config: PuzzleGeneratorConfig, seed: Long): PuzzleLevel {
        require(config.width > 0 && config.height > 0) { "Board dimensions must be positive." }
        require(config.arrowCount >= 0) { "Arrow count cannot be negative." }

        val random = Random(seed)
        val owner = Array(config.height) { IntArray(config.width) { -1 } }
        val tiles = ArrayList<ArrowTile>()
        val attemptLimit = config.width * config.height * 400
        var attempts = 0

        while (tiles.size < config.arrowCount && attempts < attemptLimit) {
            attempts++
            val tile = attemptPlacement(config, owner, random) ?: continue
            val index = tiles.size
            tile.cells.forEach { owner[it.row][it.column] = index }
            tiles += tile
        }

        return PuzzleLevel(
            id = config.id,
            pack = config.pack,
            order = config.order,
            width = config.width,
            height = config.height,
            tiles = tiles,
            parMoves = tiles.size
        )
    }

    private fun attemptPlacement(
        config: PuzzleGeneratorConfig,
        owner: Array<IntArray>,
        random: Random
    ): ArrowTile? {
        val empties = ArrayList<BoardPosition>()
        for (row in 0 until config.height) {
            for (column in 0 until config.width) {
                if (owner[row][column] == -1) empties += BoardPosition(row, column)
            }
        }
        if (empties.isEmpty()) return null

        val head = empties[random.nextInt(empties.size)]
        val maxLen = config.maxBodyCells.coerceAtLeast(1)
        val targetLen = if (maxLen == 1) 1 else random.nextInt(1, maxLen + 1)
        if (targetLen == 1) return singleCell(config, owner, random, head)

        // Grow the body backwards from the head through empty cells. Never
        // revisiting a cell prevents doubling back, and biasing toward a change
        // of direction makes the body a basic shape (L, Z, U, S) instead of a
        // long straight bar.
        val body = ArrayList<BoardPosition>()
        body += head
        var step: Direction? = null
        while (body.size < targetLen) {
            val current = body.last()
            val candidates = Direction.entries.filter { direction ->
                val next = current.step(direction)
                isEmpty(owner, next, config.width, config.height) && next !in body
            }
            if (candidates.isEmpty()) break
            val turning = candidates.filter { it != step }
            val pool = if (turning.isNotEmpty() && random.nextFloat() < 0.7f) turning else candidates
            val chosen = pool.random(random)
            body += current.step(chosen)
            step = chosen
        }

        if (body.size < 2) return singleCell(config, owner, random, head)

        // The head is last; its exit direction is the final body segment.
        val exit = directionBetween(body[1], body[0]) ?: return null
        if (!rayClear(owner, head, exit, config.width, config.height)) return null
        return ArrowTile(cells = body.reversed().toList(), direction = exit)
    }

    private fun singleCell(
        config: PuzzleGeneratorConfig,
        owner: Array<IntArray>,
        random: Random,
        head: BoardPosition
    ): ArrowTile? {
        val direction = Direction.entries
            .filter { rayClear(owner, head, it, config.width, config.height) }
            .randomOrNull(random)
            ?: return null
        return ArrowTile(cells = listOf(head), direction = direction)
    }

    private fun isEmpty(
        owner: Array<IntArray>,
        position: BoardPosition,
        width: Int,
        height: Int
    ): Boolean =
        position.inBounds(width, height) && owner[position.row][position.column] == -1

    /** True when every cell from just ahead of [head] to the edge is empty. */
    private fun rayClear(
        owner: Array<IntArray>,
        head: BoardPosition,
        direction: Direction,
        width: Int,
        height: Int
    ): Boolean {
        var position = head.step(direction)
        while (position.inBounds(width, height)) {
            if (owner[position.row][position.column] != -1) return false
            position = position.step(direction)
        }
        return true
    }
}

/** The direction from [from] to an adjacent [to], or `null` when not adjacent. */
private fun directionBetween(from: BoardPosition, to: BoardPosition): Direction? =
    Direction.entries.firstOrNull {
        from.row + it.rowDelta == to.row && from.column + it.columnDelta == to.column
    }

private fun BoardPosition.step(direction: Direction): BoardPosition =
    BoardPosition(row + direction.rowDelta, column + direction.columnDelta)

private fun BoardPosition.inBounds(width: Int, height: Int): Boolean =
    row in 0 until height && column in 0 until width

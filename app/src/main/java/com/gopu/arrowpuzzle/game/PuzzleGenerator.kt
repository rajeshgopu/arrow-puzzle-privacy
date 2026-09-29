package com.gopu.arrowpuzzle.game

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

        // Prefer bent arrows once bodies may have three cells: the turn happens
        // at an inner cell and the final segment into the head stays straight.
        if (maxLen >= 3 && random.nextFloat() < 0.6f) {
            attemptBent(config, owner, random, head, maxLen)?.let { return it }
        }
        return attemptStraight(config, owner, random, head, maxLen)
    }

    /** Builds an L-shaped arrow: straight run into the head, one clear turn behind it. */
    private fun attemptBent(
        config: PuzzleGeneratorConfig,
        owner: Array<IntArray>,
        random: Random,
        head: BoardPosition,
        maxLen: Int
    ): ArrowTile? {
        val exit = Direction.entries
            .filter { rayClear(owner, head, it, config.width, config.height) }
            .randomOrNull(random)
            ?: return null

        val previous = head.step(exit.opposite())
        if (!isEmpty(owner, previous, config.width, config.height)) return null

        val turn = exit.perpendicular().randomOrNull(random) ?: return null
        val tail = previous.step(turn)
        if (!isEmpty(owner, tail, config.width, config.height)) return null

        val cells = ArrayList<BoardPosition>()
        if (maxLen >= 4) {
            val extra = tail.step(turn)
            if (isEmpty(owner, extra, config.width, config.height)) cells += extra
        }
        cells += tail
        cells += previous
        cells += head
        return ArrowTile(cells = cells, direction = exit)
    }

    private fun attemptStraight(
        config: PuzzleGeneratorConfig,
        owner: Array<IntArray>,
        random: Random,
        head: BoardPosition,
        maxLen: Int
    ): ArrowTile? {
        val exit = Direction.entries
            .filter { rayClear(owner, head, it, config.width, config.height) }
            .randomOrNull(random)
            ?: return null

        if (maxLen >= 2 && random.nextBoolean()) {
            val previous = head.step(exit.opposite())
            if (isEmpty(owner, previous, config.width, config.height)) {
                return ArrowTile(cells = listOf(previous, head), direction = exit)
            }
        }
        return ArrowTile(cells = listOf(head), direction = exit)
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

/** The direction pointing the other way. */
private fun Direction.opposite(): Direction = when (this) {
    Direction.UP -> Direction.DOWN
    Direction.DOWN -> Direction.UP
    Direction.LEFT -> Direction.RIGHT
    Direction.RIGHT -> Direction.LEFT
}

/** The two directions at right angles to this one. */
private fun Direction.perpendicular(): List<Direction> = when (this) {
    Direction.UP, Direction.DOWN -> listOf(Direction.LEFT, Direction.RIGHT)
    Direction.LEFT, Direction.RIGHT -> listOf(Direction.UP, Direction.DOWN)
}

private fun BoardPosition.step(direction: Direction): BoardPosition =
    BoardPosition(row + direction.rowDelta, column + direction.columnDelta)

private fun BoardPosition.inBounds(width: Int, height: Int): Boolean =
    row in 0 until height && column in 0 until width

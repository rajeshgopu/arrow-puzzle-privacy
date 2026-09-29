package com.gopu.arrowpuzzle.levels

import com.gopu.arrowpuzzle.game.PuzzleGenerator
import com.gopu.arrowpuzzle.game.PuzzleGeneratorConfig
import com.gopu.arrowpuzzle.game.PuzzleLevel
import com.gopu.arrowpuzzle.game.PuzzleReducer
import com.gopu.arrowpuzzle.game.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LevelGeneratorTest {

    @Test
    fun generatedLevelsAreSolvableAndDisjoint() {
        val configs = buildList {
            add(PuzzleGeneratorConfig(width = 5, height = 5, arrowCount = 12, maxBodyCells = 2, id = "g-5x5"))
            add(PuzzleGeneratorConfig(width = 6, height = 6, arrowCount = 18, maxBodyCells = 3, id = "g-6x6"))
            add(PuzzleGeneratorConfig(width = 7, height = 7, arrowCount = 24, maxBodyCells = 3, id = "g-7x7"))
        }

        configs.forEach { config ->
            repeat(25) { seed ->
                val level = PuzzleGenerator.generate(config, seed = seed.toLong())
                assertLevelIsSound(level)
            }
        }
    }

    @Test
    fun generatedLevelsContainBentArrows() {
        val config = PuzzleGeneratorConfig(
            width = 7, height = 7, arrowCount = 24, maxBodyCells = 4, id = "bent"
        )
        val bentTotal = (0L until 20L).sumOf { seed ->
            PuzzleGenerator.generate(config, seed)
                .tiles
                .count { it.cells.size >= 3 }
        }
        assertTrue("Expected generated levels to contain bent arrows", bentTotal > 0)
    }

    @Test
    fun regeneratePackTwoToFiveWhenRequested() {
        if (System.getenv("ARROW_PUZZLE_REGENERATE_LEVELS") != "1") return

        val directory = levelsDirectory()
        (2..5).forEach { pack ->
            var bentInPack = 0
            (1..LEVELS_PER_PACK).forEach { order ->
                val config = configFor(pack, order)
                val level = generateExact(config)
                assertLevelIsSound(level)
                bentInPack += level.tiles.count { it.cells.size >= 3 }
                val file = File(directory, "${level.id}.json")
                file.writeText(level.toLevelJson().toJson())
            }
            assertTrue("Pack $pack produced no bent arrows", bentInPack > 0)
        }
    }

    private fun assertLevelIsSound(level: PuzzleLevel) {
        val occupied = level.tiles.flatMap { it.cells }
        assertEquals("${level.id}: overlapping cells", occupied.size, occupied.distinct().size)
        assertTrue(
            "${level.id}: cell outside bounds",
            occupied.all { it.row in 0 until level.height && it.column in 0 until level.width }
        )
        assertTrue("${level.id}: no tiles", level.tiles.isNotEmpty())

        level.tiles.forEach { tile ->
            if (tile.cells.size >= 2) {
                val previous = tile.cells[tile.cells.size - 2]
                val head = tile.cells.last()
                val delta = head.row - previous.row to head.column - previous.column
                val expected = when (delta) {
                    -1 to 0 -> "UP"
                    1 to 0 -> "DOWN"
                    0 to -1 -> "LEFT"
                    0 to 1 -> "RIGHT"
                    else -> "?"
                }
                assertEquals("${level.id}: bend does not match direction", expected, tile.direction.name)
            }
        }

        assertTrue("${level.id}: unsolvable", isSolvable(level))
    }

    private fun isSolvable(level: PuzzleLevel): Boolean {
        var state = PuzzleReducer.start(level)
        var guard = level.tiles.size + 1
        while (state.remainingTiles.isNotEmpty()) {
            if (guard-- <= 0) return false
            val move = PuzzleReducer.firstValidMove(state) ?: return false
            val next: PuzzleState = PuzzleReducer.tap(state, move)
            if (next.remainingTiles.size >= state.remainingTiles.size) return false
            state = next
        }
        return true
    }

    private fun generateExact(config: PuzzleGeneratorConfig): PuzzleLevel {
        val base = config.pack * 100_000L + config.order * 1_000L
        repeat(300) { attempt ->
            val level = PuzzleGenerator.generate(config, seed = base + attempt)
            if (level.tiles.size == config.arrowCount) return level
        }
        return PuzzleGenerator.generate(config, seed = base)
    }

    private fun configFor(pack: Int, order: Int): PuzzleGeneratorConfig {
        val id = "pack-0$pack-level-${String.format("%02d", order)}"
        return when (pack) {
            2 -> PuzzleGeneratorConfig(
                width = 5, height = 5, arrowCount = 7 + order, maxBodyCells = 3,
                id = id, pack = pack, order = order
            )
            3 -> PuzzleGeneratorConfig(
                width = 6, height = 6, arrowCount = 9 + order, maxBodyCells = 3,
                id = id, pack = pack, order = order
            )
            4 -> PuzzleGeneratorConfig(
                width = 6, height = 6, arrowCount = 13 + order, maxBodyCells = 4,
                id = id, pack = pack, order = order
            )
            else -> PuzzleGeneratorConfig(
                width = 7, height = 7, arrowCount = 16 + order, maxBodyCells = 4,
                id = id, pack = pack, order = order
            )
        }
    }

    private fun levelsDirectory(): File {
        val candidates = listOf(
            File("src/main/assets/levels"),
            File("app/src/main/assets/levels"),
            File(System.getProperty("user.dir"), "src/main/assets/levels")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Level assets directory not found. user.dir=${System.getProperty("user.dir")}")
    }
}

package com.gopu.arrow.puzzle.game.levels

import com.gopu.arrow.puzzle.game.GenerationRequest
import com.gopu.arrow.puzzle.game.PuzzleGenerator
import com.gopu.arrow.puzzle.game.PuzzleGeneratorConfig
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.PuzzleReducer
import com.gopu.arrow.puzzle.game.PuzzleState
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

        val requests = configs.flatMap { config ->
            (0L until 25L).map { seed -> GenerationRequest(config = config, seed = seed) }
        }
        PuzzleGenerator.generateAllBlocking(requests).forEach(::assertLevelIsSound)
    }

    @Test
    fun generatedLevelsContainBentArrows() {
        val config = PuzzleGeneratorConfig(
            width = 7, height = 7, arrowCount = 24, maxBodyCells = 4, id = "bent"
        )
        val bentTotal = PuzzleGenerator
            .generateAllBlocking((0L until 20L).map { seed -> GenerationRequest(config = config, seed = seed) })
            .sumOf { level -> level.tiles.count { it.cells.size >= 3 } }
        assertTrue("Expected generated levels to contain bent arrows", bentTotal > 0)
    }

    @Test
    fun parallelBatchMatchesOneSeedAtATime() {
        val config = configFor(4, 3)
        val requests = (0L until 12L).map { seed -> GenerationRequest(config = config, seed = seed) }

        val parallel = PuzzleGenerator.generateAllBlocking(requests)
        val sequential = requests.map { request ->
            PuzzleGenerator.generate(request.config, request.seed)
        }

        assertEquals(sequential.size, parallel.size)
        parallel.forEachIndexed { index, level ->
            val expected = sequential[index].toLevelJson().toJson()
            assertEquals("batch order drifted at $index", expected, level.toLevelJson().toJson())
        }
    }

    @Test
    fun parallelExactSearchMatchesTheSequentialSeedWalk() {
        val config = configFor(5, 4)
        val request = GenerationRequest(
            config = config,
            seed = config.pack * 100_000L + config.order * 1_000L,
            requireExactArrowCount = true
        )

        // The sequential rule the parallel wave replaces: lowest seed first.
        val expected = (0 until 300).asSequence()
            .map { attempt -> PuzzleGenerator.generate(config, request.seed + attempt) }
            .firstOrNull { it.tiles.size == config.arrowCount }
            ?: PuzzleGenerator.generate(config, request.seed)

        val actual = PuzzleGenerator.generateAllBlocking(listOf(request)).single()
        assertEquals(expected.toLevelJson().toJson(), actual.toLevelJson().toJson())
        assertEquals(config.arrowCount, actual.tiles.size)
    }

    @Test
    fun regeneratePackTwoToFiveWhenRequested() {
        if (System.getenv("ARROW_PUZZLE_REGENERATE_LEVELS") != "1") return

        val directory = levelsDirectory()
        val requests = (2..5).flatMap { pack ->
            (1..LEVELS_PER_PACK).map { order ->
                val config = configFor(pack, order)
                GenerationRequest(
                    config = config,
                    seed = config.pack * 100_000L + config.order * 1_000L,
                    requireExactArrowCount = true
                )
            }
        }

        val levels = PuzzleGenerator.generateAllBlocking(requests)

        val bentByPack = levels
            .onEach(::assertLevelIsSound)
            .groupBy({ it.pack }, { it.tiles.count { tile -> tile.cells.size >= 3 } })
            .mapValues { (_, counts) -> counts.sum() }

        levels.forEach { level -> File(directory, "${level.id}.json").writeText(level.toLevelJson().toJson()) }

        bentByPack.forEach { (pack, bent) ->
            assertTrue("Pack $pack produced no bent arrows", bent > 0)
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

    /**
     * Board sizes mirror `tools/generate-levels.ps1`: portrait boards in the
     * same band as the gameplay play area, so the grid fills the plate instead
     * of letterboxing inside it.
     */
    private fun configFor(pack: Int, order: Int): PuzzleGeneratorConfig {
        val id = "pack-0$pack-level-${String.format("%02d", order)}"
        // Ramp board size, arrow count and longest body across the packs, and
        // nudge each within a pack, so difficulty climbs level by level.
        return when (pack) {
            2 -> if (order <= 3) {
                PuzzleGeneratorConfig(
                    width = 4, height = 7, arrowCount = 9 + order,
                    maxBodyCells = 2 + (order - 1) / 6, id = id, pack = pack, order = order
                )
            } else {
                PuzzleGeneratorConfig(
                    width = 5, height = 8, arrowCount = 13 + order,
                    maxBodyCells = 2 + (order - 1) / 4, id = id, pack = pack, order = order
                )
            }
            3 -> if (order <= 4) {
                PuzzleGeneratorConfig(
                    width = 5, height = 8, arrowCount = 13 + order,
                    maxBodyCells = 2 + (order - 1) / 4, id = id, pack = pack, order = order
                )
            } else {
                PuzzleGeneratorConfig(
                    width = 5, height = 9, arrowCount = 15 + order,
                    maxBodyCells = 3 + (order - 1) / 6, id = id, pack = pack, order = order
                )
            }
            4 -> if (order <= 3) {
                PuzzleGeneratorConfig(
                    width = 5, height = 9, arrowCount = 15 + order,
                    maxBodyCells = 3 + (order - 1) / 6, id = id, pack = pack, order = order
                )
            } else {
                PuzzleGeneratorConfig(
                    width = 6, height = 10, arrowCount = 17 + order,
                    maxBodyCells = 3 + (order - 1) / 4, id = id, pack = pack, order = order
                )
            }
            else -> PuzzleGeneratorConfig(
                width = 7, height = 11, arrowCount = 23 + order,
                maxBodyCells = 3 + (order - 1) / 4, id = id, pack = pack, order = order
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

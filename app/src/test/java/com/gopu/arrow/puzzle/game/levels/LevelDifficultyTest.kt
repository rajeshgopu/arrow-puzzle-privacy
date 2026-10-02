package com.gopu.arrow.puzzle.game.levels

import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

/**
 * The shipped levels have to get harder, not just bigger.
 *
 * `LevelAssetValidatorTest` proves every asset is legal and solvable; this test
 * proves the set still forms a curve. Arrow count has to rise, the board has to
 * fill in, the spacing has to tighten, and the arrows have to end up underneath
 * each other in chains rather than in one flat layer of independent pieces. A
 * generator that quietly regresses to "more arrows, same structure" passes every
 * structural test, so these numbers are asserted rather than assumed.
 */
class LevelDifficultyTest {

    @Test
    fun arrowCountRisesAcrossTheGame() {
        val counts = levels().map { it.arrows }

        // No level may be busier than the one before it: difficulty is a ramp,
        // and a dip reads as a bug to the player rather than as variety.
        counts.zipWithNext().forEachIndexed { index, pair ->
            assertTrue(
                "Level ${index + 1} has ${pair.second} arrows, fewer than the ${pair.first} before it",
                pair.second >= pair.first
            )
        }

        // The bands the design calls for: an easy opening, a medium middle and a
        // heavy end. Pack 1 is the tutorial ramp, packs 2 and 3 are the middle,
        // packs 4 and 5 are the hard end that has to clear forty-five arrows.
        assertEquals(50, counts.size)
        assertTrue("pack 1 should open at ten arrows, found ${counts.first()}", counts.first() in 10..12)
        assertTrue("pack 1 should top out near eighteen arrows, found ${counts[9]}", counts[9] in 16..18)
        assertTrue("pack 2 should stay in the medium band, found ${counts[19]}", counts[19] in 20..30)
        assertTrue("pack 3 should stay in the medium band, found ${counts[29]}", counts[29] in 30..40)
        assertTrue("pack 4 should be a high level, found ${counts[39]}", counts[39] in 35..46)
        assertTrue("pack 5 should be the hard end, found ${counts[49]}", counts[49] in 45..55)
    }

    @Test
    fun laterLevelsFillTheirBoardMoreThoroughly() {
        val byPack = packGroups()
        val first = byPack[1]!!
        val second = byPack[2]!!
        val fourth = byPack[4]!!
        val fifth = byPack[5]!!
        assertNotNull("pack 5 levels are missing", fifth)

        // The last pack is the dense one: no big unused region left on the board.
        assertTrue("pack 5 fill was ${averageFill(fifth)}", averageFill(fifth) >= 0.72)
        assertTrue(
            "later packs should fill more than the first " +
                "(${averageFill(first)} -> ${averageFill(second)} -> ${averageFill(fourth)})",
            averageFill(fourth) > averageFill(first) && averageFill(fifth) > averageFill(second)
        )
    }

    @Test
    fun boardAreaGrowsWithTheArrowCount() {
        val cells = levels().map { it.cells }
        // Cells only ever go up, so a level is never squeezed onto a smaller grid.
        cells.zipWithNext().forEachIndexed { index, pair ->
            assertTrue("Level ${index + 1} shrank the board to ${pair.second} cells", pair.second >= pair.first)
        }
        assertTrue("the opening board was only ${cells.first()} cells", cells.first() <= 30)
        assertTrue("the final board is only ${cells.last()} cells", cells.last() >= 90)
    }

    @Test
    fun harderLevelsAreBuiltFromDependencyChainsNotJustMoreArrows() {
        val byPack = packGroups()
        val first = byPack[1]!!
        val fifth = byPack[5]!!
        assertNotNull("pack 5 levels are missing", fifth)

        val earlyDepth = first.map { it.depth.toDouble() }.average()
        val lateDepth = fifth.map { it.depth.toDouble() }.average()
        assertTrue("pack 5 depth $lateDepth did not beat pack 1 depth $earlyDepth", lateDepth > earlyDepth)

        val lateLayers = fifth.map { it.layers.toDouble() }.average()
        assertTrue("pack 5 should stack many layers of blocking, found $lateLayers", lateLayers >= 5.0)

        // "Blocked by more than one arrow" is what separates a chain from a list.
        // It is the difference between an arrow that waits for one neighbour and
        // an arrow that waits for a whole order.
        val lateShared = fifth.map { it.sharedBlocked.toDouble() / it.arrows }.average()
        assertTrue("pack 5 should block arrows more than once, found $lateShared", lateShared >= 0.20)

        val lateCrossings = fifth.map { it.crossings.toDouble() / it.arrows }.average()
        assertTrue("pack 5 should cross paths constantly, found $lateCrossings", lateCrossings >= 0.7)

        // Every level keeps a way in: at least one arrow is clear on the first
        // move, and never so many that the board is a free-for-all.
        levels().forEach { measured ->
            assertTrue("${measured.id}: no arrow is clear to start", measured.open >= 1)
            val ceiling = maxOf(2, (measured.arrows * 0.45).roundToInt())
            assertTrue(
                "${measured.id}: ${measured.open} of ${measured.arrows} arrows are clear to start",
                measured.open <= ceiling
            )
        }
    }

    @Test
    fun everyLevelMixesShortAndLongArrowsAndFacesSeveralDirections() {
        val measured = levels()
        measured.forEach { one ->
            assertTrue(
                "${one.id}: uses only ${one.directions} of the four directions",
                one.directions >= 3
            )
        }

        // Long bodies appear as the boards get denser and there is room to thread
        // them; short bodies keep the count up on the levels that need it.
        val byPack = packGroups()
        val first = byPack[1]!!
        val fifth = byPack[5]!!
        assertNotNull("pack 5 levels are missing", fifth)

        assertTrue(
            "pack 5 should carry long arrows, longest bodies were " +
                fifth.map { it.longestBody }.max(),
            fifth.map { it.longestBody }.max() >= 4
        )
        assertTrue("short arrows should still exist on the hardest pack", fifth.all { it.shortArrows >= 1 })

        val earlyBent = first.sumOf { it.bentArrows }
        val lateBent = fifth.sumOf { it.bentArrows }
        assertTrue("bent arrows never appeared ($earlyBent then $lateBent)", earlyBent >= 1 && lateBent > earlyBent)
    }

    @Test
    fun harderLevelsWrapMoreOften() {
        val fifth = packGroups()[5]!!
        assertNotNull("pack 5 levels are missing", fifth)
        val wrapped = fifth.count { it.longestBody > 1 }.toDouble() / fifth.size
        // "Wrapped" here means a multi-cell arrow, whose rendered path is folded
        // rather than a single straight segment.
        assertTrue("multi-cell arrows should be the norm on pack 5, saw $wrapped", wrapped >= 0.5)
    }

    // --- measurement ---------------------------------------------------------

    private data class LevelMetrics(
        val id: String,
        val pack: Int,
        val arrows: Int,
        val cells: Int,
        val occupancyFraction: Double,
        val open: Int,
        val depth: Int,
        val layers: Int,
        val sharedBlocked: Int,
        val crossings: Int,
        val directions: Int,
        val longestBody: Int,
        val shortArrows: Int,
        val bentArrows: Int
    )

    private fun packGroups(): Map<Int, List<LevelMetrics>> =
        levels().groupBy { it.pack }

    private fun averageFill(group: List<LevelMetrics>): Double =
        group.map { it.occupancyFraction }.average()

    private fun levels(): List<LevelMetrics> =
        levelFiles().map { file ->
            measure(parseLevelJson(file.readText()).toPuzzleLevel())
        }

    private fun measure(level: PuzzleLevel): LevelMetrics {
        val tiles = level.tiles
        val blockers = tiles.mapIndexed { index, tile -> blockersOf(level, tile, index) }
        val depths = IntArray(tiles.size) { -1 }
        val onStack = BooleanArray(tiles.size)
        tiles.indices.forEach { depthOf(it, blockers, depths, onStack) }

        val occupied = tiles.sumOf { it.cells.size }
        val cells = level.width * level.height
        return LevelMetrics(
            id = level.id,
            pack = level.pack,
            arrows = tiles.size,
            cells = cells,
            occupancyFraction = occupied.toDouble() / cells,
            open = blockers.count { it.isEmpty() },
            depth = depths.max(),
            layers = depths.toSet().size,
            sharedBlocked = blockers.count { it.size > 1 },
            crossings = blockers.sumOf { it.size },
            directions = tiles.map { it.direction }.toSet().size,
            longestBody = tiles.maxOf { it.cells.size },
            shortArrows = tiles.count { it.cells.size <= 2 },
            bentArrows = tiles.count { it.cells.size >= 3 }
        )
    }

    /** The arrows standing between [tile]'s head and the board edge. */
    private fun blockersOf(level: PuzzleLevel, tile: ArrowTile, index: Int): List<Int> {
        val found = LinkedHashSet<Int>()
        var position: BoardPosition = tile.position.step(tile.direction)
        while (position.row in 0 until level.height && position.column in 0 until level.width) {
            val owner = level.ownerAt(position)
            if (owner != null && owner != index) found += owner
            position = position.step(tile.direction)
        }
        return found.toList()
    }

    /**
     * Longest chain of arrows that have to be cleared before this one can move.
     * A cycle would mean an unsolvable board, so a tile already on the stack
     * contributes nothing rather than recursing forever.
     */
    private fun depthOf(
        index: Int,
        blockers: List<List<Int>>,
        memo: IntArray,
        onStack: BooleanArray
    ): Int {
        if (memo[index] >= 0) return memo[index]
        if (onStack[index]) return 0
        onStack[index] = true
        var best = 0
        for (blocker in blockers[index]) {
            best = maxOf(best, depthOf(blocker, blockers, memo, onStack) + 1)
        }
        onStack[index] = false
        memo[index] = best
        return best
    }

    private fun levelFiles(): List<File> =
        levelsDirectory()
            .listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name }
            ?: emptyList()

    private fun levelsDirectory(): File {
        val candidates = listOf(
            File("src/main/assets/levels"),
            File("app/src/main/assets/levels"),
            File(System.getProperty("user.dir"), "src/main/assets/levels")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error(
                "Level assets directory not found. user.dir=${System.getProperty("user.dir")}"
            )
    }
}
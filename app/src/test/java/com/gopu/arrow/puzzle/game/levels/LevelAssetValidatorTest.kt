package com.gopu.arrow.puzzle.game.levels

import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.PuzzleReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LevelAssetValidatorTest {

    private val validDirections = setOf("UP", "RIGHT", "DOWN", "LEFT")

    // Renderer geometry in cell units, mirroring ArrowShape. Tube plus keyline is
    // about 0.164 cells wide, so 0.20 keeps a visible sliver of background
    // between neighbouring arrows and their arrowheads.
    private val MinRenderedClearance = 0.20
    private val StubReach = 0.5
    private val HeadOverlap = 0.025
    private val HeadLength = 0.28
    private val HeadWidth = 0.20

    @Test
    fun atLeastTenPackOneLevelsAreShipped() {
        val files = levelFiles()
        val packOne = files.filter { it.nameWithoutExtension.startsWith("pack-01-") }

        assertTrue(
            "Expected at least 10 pack-1 levels, found ${packOne.size}",
            packOne.size >= 10
        )
    }

    @Test
    fun fiftyLevelsAreShippedAcrossFivePacks() {
        val files = levelFiles()
        assertEquals("Expected 50 level assets", 50, files.size)

        (1..5).forEach { pack ->
            val prefix = "pack-0$pack-"
            val count = files.count { it.nameWithoutExtension.startsWith(prefix) }
            assertEquals("Expected 10 levels in pack $pack", 10, count)
        }
    }

    @Test
    fun everyShippedLevelKeepsRenderedArrowsVisuallySeparated() {
        val offenders = levelFiles().mapNotNull { file ->
            val level = parseLevelJson(file.readText()).toPuzzleLevel()
            val (distance, pair) = tightestRenderedPair(level)
            if (distance < MinRenderedClearance) {
                "${file.name}: ${format(distance)} cells apart (arrows $pair)"
            } else {
                null
            }
        }
        assertTrue(
            "rendered arrow tubes must stay at least ${format(MinRenderedClearance)} cells apart:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    @Test
    fun debugPrintClosestPair() {
        val file = levelFiles().first { it.name == "pack-01-level-02.json" }
        val level = parseLevelJson(file.readText()).toPuzzleLevel()
        println("tiles=" + level.tiles.size)
        level.tiles.forEachIndexed { index, tile ->
            println(
                "tile $index head=${tile.position.row},${tile.position.column} " +
                    "dir=${tile.direction} cells=${tile.cells.size} " +
                    "footprint=" + renderedFootprint(tile).joinToString(" ") {
                        "(" + format(it[0]) + "," + format(it[1]) + ")"
                    }
            )
        }
        val (distance, pair) = tightestRenderedPair(level)
        println("closest=$distance pair=$pair")
    }

    @Test
    fun everyShippedLevelIsStructurallyValid() {
        val files = levelFiles()
        assertTrue("No level assets found at ${levelsDirectory()}", files.isNotEmpty())

        files.forEach { file ->
            val json = file.readText()
            val level = parseLevelJson(json)

            assertTrue("${file.name}: width must be positive", level.width > 0)
            assertTrue("${file.name}: height must be positive", level.height > 0)
            assertTrue("${file.name}: must contain tiles", level.tiles.isNotEmpty())

            val positions = level.tiles.map { it.row to it.column }
            assertEquals(
                "${file.name}: contains overlapping tiles",
                positions.size,
                positions.distinct().size
            )
            assertTrue(
                "${file.name}: contains a tile outside the board bounds",
                level.tiles.all { it.row in 0 until level.height && it.column in 0 until level.width }
            )
            assertTrue(
                "${file.name}: contains an unsupported direction",
                level.tiles.all { it.direction in validDirections }
            )
            assertTrue("${file.name}: failed shared validateLevel", validateLevel(level))
            assertEquals(
                "${file.name}: id must match the file name",
                file.nameWithoutExtension,
                level.id
            )
        }
    }

    @Test
    fun everyShippedLevelIsSolvable() {
        val unsolvable = levelFiles()
            .filterNot { file ->
                isSolvable(parseLevelJson(file.readText()).toPuzzleLevel())
            }
            .map { it.name }

        assertTrue("Unsolvable levels: $unsolvable", unsolvable.isEmpty())
    }

    private fun isSolvable(level: PuzzleLevel): Boolean {
        var state = PuzzleReducer.start(level)
        var guard = level.tiles.size + 1
        while (state.remainingTiles.isNotEmpty()) {
            if (guard-- <= 0) return false
            val move = PuzzleReducer.firstValidMove(state) ?: return false
            val next = PuzzleReducer.tap(state, move)
            if (next.remainingTiles.size >= state.remainingTiles.size) return false
            state = next
        }
        return true
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

    /**
     * Closest approach, in cells, between the rendered centre-lines of any two
     * arrows in the level, plus which pair produced it.
     *
     * The footprint deliberately uses the un-rounded body spine plus the tail
     * stub and the arrowhead extremes, which is what the renderer draws on top
     * of it. Rounded bends cut inside that spine, so using the spine is
     * conservative: anything the test passes also has clearance once the bends
     * are applied.
     */
    private fun tightestRenderedPair(level: PuzzleLevel): Pair<Double, String> {
        var best = Double.MAX_VALUE
        var pair = "none"
        val footprints = level.tiles.map(::renderedFootprint)
        for (a in footprints.indices) {
            for (b in a + 1 until footprints.size) {
                val distance = closestApproach(footprints[a], footprints[b])
                if (distance < best) {
                    best = distance
                    pair = "${level.tiles[a].position.row},${level.tiles[a].position.column} / " +
                        "${level.tiles[b].position.row},${level.tiles[b].position.column}"
                }
            }
        }
        return best to pair
    }

    /** Tail stub, body spine, and the widest points of the arrowhead. */
    private fun renderedFootprint(tile: com.gopu.arrow.puzzle.game.ArrowTile): List<DoubleArray> {
        val points = ArrayList<DoubleArray>()
        val cells = tile.cells
        val fx = tile.direction.columnDelta.toDouble()
        val fy = tile.direction.rowDelta.toDouble()
        val head = cells.last()
        val headX = head.column.toDouble()
        val headY = head.row.toDouble()
        val baseX = headX - fx * HeadOverlap
        val baseY = headY - fy * HeadOverlap

        if (cells.size == 1) {
            points += doubleArrayOf(baseX - fx * StubReach, baseY - fy * StubReach)
        } else {
            val tail = cells.first()
            val next = cells[1]
            val dx = (tail.column - next.column).toDouble()
            val dy = (tail.row - next.row).toDouble()
            val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001)
            points += doubleArrayOf(
                tail.column + dx / length * StubReach,
                tail.row + dy / length * StubReach
            )
            for (index in 0 until cells.size - 1) {
                points += doubleArrayOf(cells[index].column.toDouble(), cells[index].row.toDouble())
            }
        }
        points += doubleArrayOf(baseX, baseY)

        val perpX = -fy
        val perpY = fx
        val wingOffset = HeadLength * 0.32
        val wingHalf = HeadWidth * 0.5
        points += doubleArrayOf(baseX + fx * HeadLength, baseY + fy * HeadLength)
        points += doubleArrayOf(baseX + fx * wingOffset + perpX * wingHalf, baseY + fy * wingOffset + perpY * wingHalf)
        points += doubleArrayOf(baseX + fx * wingOffset - perpX * wingHalf, baseY + fy * wingOffset - perpY * wingHalf)
        return points
    }

    private fun closestApproach(a: List<DoubleArray>, b: List<DoubleArray>): Double {
        var best = Double.MAX_VALUE
        // Segment-to-segment: check every vertex of each polyline against the
        // other's segments, so a mid-segment crossing is not missed.
        for (i in 0 until a.size) {
            for (j in 0 until b.size - 1) {
                val distance = pointToSegment(
                    a[i][0], a[i][1],
                    b[j][0], b[j][1], b[j + 1][0], b[j + 1][1]
                )
                if (distance < best) best = distance
            }
        }
        for (j in 0 until b.size) {
            for (i in 0 until a.size - 1) {
                val distance = pointToSegment(
                    b[j][0], b[j][1],
                    a[i][0], a[i][1], a[i + 1][0], a[i + 1][1]
                )
                if (distance < best) best = distance
            }
        }
        return best
    }

    private fun pointToSegment(
        px: Double, py: Double,
        ax: Double, ay: Double,
        bx: Double, by: Double
    ): Double {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared < 1e-9) return kotlin.math.hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return kotlin.math.hypot(px - (ax + dx * t), py - (ay + dy * t))
    }

    private fun format(value: Double): String = String.format("%.3f", value)
}

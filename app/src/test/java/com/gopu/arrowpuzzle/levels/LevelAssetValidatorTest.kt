package com.gopu.arrowpuzzle.levels

import com.gopu.arrowpuzzle.game.PuzzleLevel
import com.gopu.arrowpuzzle.game.PuzzleReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LevelAssetValidatorTest {

    private val validDirections = setOf("UP", "RIGHT", "DOWN", "LEFT")

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
}

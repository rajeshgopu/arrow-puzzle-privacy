package com.gopu.arrow.puzzle.game.levels

import android.content.Context
import com.gopu.arrow.puzzle.game.PuzzleLevel

class LevelRepository(private val context: Context) {
    fun loadLevel(pack: Int, order: Int): PuzzleLevel? {
        val fileName = "pack-0${pack}-level-${String.format("%02d", order)}.json"
        return try {
            val inputStream = context.assets.open("levels/$fileName")
            val json = inputStream.bufferedReader().use { it.readText() }
            parseLevel(json)
        } catch (e: Exception) {
            null
        }
    }

    fun loadAllLevels(pack: Int, count: Int): List<PuzzleLevel> {
        return (1..count).mapNotNull { loadLevel(pack, it) }
    }

    private fun parseLevel(json: String): PuzzleLevel? {
        val levelJson = parseLevelJson(json)
        if (!validateLevel(levelJson)) return null
        return runCatching { levelJson.toPuzzleLevel() }.getOrNull()
    }
}

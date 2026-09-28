package com.gopu.arrowpuzzle.levels

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException

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
    val direction: String
)

fun parseLevelJson(json: String): LevelJson {
    return Gson().fromJson(json, LevelJson::class.java)
}

fun validateLevel(level: LevelJson): Boolean {
    if (level.width <= 0 || level.height <= 0) return false
    if (level.tiles.isEmpty()) return false
    if (level.tiles.size > level.width * level.height) return false
    val positions = level.tiles.map { it.row to it.column }
    if (positions.distinct().size != positions.size) return false
    if (!level.tiles.all { it.row in 0 until level.height && it.column in 0 until level.width }) return false
    val validDirections = setOf("UP", "RIGHT", "DOWN", "LEFT")
    if (!level.tiles.all { it.direction in validDirections }) return false
    return true
}

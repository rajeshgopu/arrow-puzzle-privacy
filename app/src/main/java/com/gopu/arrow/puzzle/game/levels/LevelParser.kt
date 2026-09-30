package com.gopu.arrow.puzzle.game.levels

import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class LevelParser {
    private val gson = Gson()

    fun parse(json: String): PuzzleLevel {
        val obj = gson.fromJson(json, JsonObject::class.java)
        val id = obj.get("id").asString
        val pack = obj.get("pack").asInt
        val order = obj.get("order").asInt
        val width = obj.get("width").asInt
        val height = obj.get("height").asInt
        val tilesArray = obj.get("tiles").asJsonArray
        val parMoves = obj.get("parMoves").asInt

        val tiles = mutableListOf<ArrowTile>()
        for (i in 0 until tilesArray.size()) {
            val tileObj = tilesArray[i].asJsonObject
            val row = tileObj.get("row").asInt
            val column = tileObj.get("column").asInt
            val direction = Direction.valueOf(tileObj.get("direction").asString)
            tiles.add(ArrowTile(BoardPosition(row, column), direction))
        }

        return PuzzleLevel(id = id, pack = pack, order = order, width = width, height = height, tiles = tiles, parMoves = parMoves)
    }
}

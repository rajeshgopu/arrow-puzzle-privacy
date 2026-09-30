package com.gopu.arrow.puzzle.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleReducerTest {
    private val rightEdge = BoardPosition(0, 1)
    private val blockedTile = BoardPosition(0, 0)
    private val level = PuzzleLevel(
        id = "test-level",
        pack = 1,
        order = 1,
        width = 2,
        height = 2,
        tiles = listOf(
            ArrowTile(blockedTile, Direction.RIGHT),
            ArrowTile(rightEdge, Direction.RIGHT)
        )
    )

    @Test
    fun blockedArrowCostsOneLifeAndRemainsOnBoard() {
        val result = PuzzleReducer.tap(PuzzleReducer.start(level), blockedTile)

        assertEquals(2, result.lives)
        assertEquals(1, result.invalidTaps)
        assertEquals(GameStatus.PLAYING, result.status)
        assertTrue(blockedTile in result.remainingTiles)
    }

    @Test
    fun clearArrowIsRemovedWithoutCostingALife() {
        val result = PuzzleReducer.tap(PuzzleReducer.start(level), rightEdge)

        assertEquals(3, result.lives)
        assertEquals(0, result.invalidTaps)
        assertFalse(rightEdge in result.remainingTiles)
        assertTrue(blockedTile in result.remainingTiles)
    }

    @Test
    fun removingTheBlockerAllowsTheFinalArrowAndWins() {
        val afterFirstTap = PuzzleReducer.tap(PuzzleReducer.start(level), rightEdge)
        val result = PuzzleReducer.tap(afterFirstTap, blockedTile)

        assertTrue(result.remainingTiles.isEmpty())
        assertEquals(GameStatus.WON, result.status)
    }

    @Test
    fun threeInvalidTapsLoseTheLevel() {
        var state = PuzzleReducer.start(level)
        repeat(3) { state = PuzzleReducer.tap(state, blockedTile) }

        assertEquals(0, state.lives)
        assertEquals(GameStatus.LOST, state.status)
        assertTrue(blockedTile in state.remainingTiles)
    }

    @Test(expected = IllegalArgumentException::class)
    fun overlappingTilesAreRejected() {
        PuzzleLevel(
            id = "invalid",
            pack = 1,
            order = 1,
            width = 2,
            height = 2,
            tiles = listOf(
                ArrowTile(BoardPosition(0, 0), Direction.UP),
                ArrowTile(BoardPosition(0, 0), Direction.DOWN)
            )
        )
    }

    @Test
    fun firstValidMoveReturnsAClearArrow() {
        val target = PuzzleReducer.firstValidMove(PuzzleReducer.start(level))

        assertEquals(rightEdge, target)
    }

    @Test
    fun firstValidMoveIsNullWhenNothingIsClear() {
        val blocked = PuzzleLevel(
            id = "blocked",
            pack = 1,
            order = 1,
            width = 2,
            height = 2,
            tiles = listOf(
                ArrowTile(BoardPosition(0, 0), Direction.RIGHT),
                ArrowTile(BoardPosition(0, 1), Direction.LEFT)
            )
        )

        assertEquals(null, PuzzleReducer.firstValidMove(PuzzleReducer.start(blocked)))
    }

    @Test
    fun starsReflectInvalidTapCount() {
        assertEquals(3, starsForInvalidTaps(0))
        assertEquals(2, starsForInvalidTaps(1))
        assertEquals(1, starsForInvalidTaps(2))
        assertEquals(1, starsForInvalidTaps(9))
    }
}

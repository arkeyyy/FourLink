package com.example.fourlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    @Test
    fun validDropsStackFromBottomAndAlternatePlayers() {
        val game = GameState()

        assertEquals(Cell(5, 0), game.place(0).cell)
        assertEquals(Player.RED, game.currentPlayer)
        assertEquals(Cell(5, 1), game.place(1).cell)
        assertEquals(Player.YELLOW, game.currentPlayer)
        assertEquals(Cell(4, 0), game.place(0).cell)
        assertFalse(game.isFinished)
    }

    @Test
    fun fullColumnDoesNotChangeTurn() {
        val game = GameState()
        repeat(GameState.ROWS) { game.place(0) }

        val player = game.currentPlayer
        assertEquals(MoveResult.ColumnFull, game.drop(0))
        assertEquals(player, game.currentPlayer)
        assertFalse(game.isFinished)
    }

    @Test
    fun horizontalWinStopsFurtherMoves() {
        val game = GameState()
        val result = game.play(0, 0, 1, 1, 2, 2, 3)

        assertEquals(Player.YELLOW, result.player)
        assertEquals((0..3).map { Cell(5, it) }.toSet(), result.winningCells.toSet())
        assertTrue(game.isFinished)
        assertEquals(MoveResult.GameAlreadyEnded, game.drop(4))
    }

    @Test
    fun verticalWinIsDetected() {
        val game = GameState()
        val result = game.play(0, 1, 0, 1, 0, 1, 0)

        assertEquals((2..5).map { Cell(it, 0) }.toSet(), result.winningCells.toSet())
    }

    @Test
    fun bothDiagonalDirectionsAreDetected() {
        val ascending = intArrayOf(0, 1, 1, 2, 4, 2, 2, 3, 4, 3, 5, 3, 3)
        for (sequence in listOf(ascending, ascending.map { 6 - it }.toIntArray())) {
            val game = GameState()
            val result = game.play(*sequence)
            assertEquals(4, result.winningCells.size)
            assertEquals(Player.YELLOW, result.player)
            assertTrue(game.isFinished)
        }
    }

    @Test
    fun fullBoardWithoutWinnerIsDraw() {
        val sequence = intArrayOf(
            2, 6, 4, 1, 4, 3, 4, 3, 0, 1, 1, 4, 4, 4,
            6, 0, 0, 6, 2, 5, 5, 6, 1, 2, 1, 2, 3, 0,
            3, 6, 0, 3, 0, 3, 6, 2, 5, 5, 2, 1, 5, 5
        )
        val game = GameState()
        sequence.dropLast(1).forEach { column ->
            val move = game.place(column)
            assertFalse(move.isDraw)
            assertTrue(move.winningCells.isEmpty())
        }

        val lastMove = game.place(sequence.last())
        assertTrue(lastMove.isDraw)
        assertTrue(lastMove.winningCells.isEmpty())
        assertTrue(game.isFinished)
    }

    @Test
    fun surrenderAwardsOpponentAndStopsMoves() {
        val game = GameState()
        game.place(0)

        assertEquals(Player.YELLOW, game.surrender())
        assertTrue(game.isFinished)
        assertEquals(null, game.surrender())
        assertEquals(MoveResult.GameAlreadyEnded, game.drop(1))
    }

    @Test
    fun restoredMatchContinuesFromItsOwnSnapshot() {
        val original = GameState()
        original.play(0, 1, 0)
        val restored = GameState().apply { restore(original.snapshot()) }
        assertEquals(Player.RED, restored.currentPlayer)
        assertEquals(Cell(4, 1), restored.place(1).cell)
        assertEquals(null, original.playerAt(Cell(4, 1)))
        assertEquals(Player.YELLOW, restored.playerAt(Cell(4, 0)))
    }

    @Test
    fun restoredWinAndSurrenderRemainFinished() {
        val won = GameState().apply { play(0, 0, 1, 1, 2, 2, 3) }
        val restored = GameState().apply { restore(won.snapshot()) }
        assertEquals(Player.YELLOW, restored.winner)
        assertEquals(won.winningCells, restored.winningCells)
        assertEquals(MoveResult.GameAlreadyEnded, restored.drop(5))
        val surrendered = GameState().apply { surrender() }
        restored.restore(surrendered.snapshot())
        assertEquals(Player.RED, restored.winner)
        assertTrue(restored.winningCells.isEmpty())
        assertTrue(restored.isFinished)
    }

    private fun GameState.place(column: Int): MoveResult.Placed =
        drop(column) as MoveResult.Placed

    private fun GameState.play(vararg columns: Int): MoveResult.Placed {
        var lastMove: MoveResult.Placed? = null
        columns.forEach { lastMove = place(it) }
        return requireNotNull(lastMove)
    }
}

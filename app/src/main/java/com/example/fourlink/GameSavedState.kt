package com.example.fourlink

import android.os.Bundle

internal object GameSavedState {
    fun write(out: Bundle, game: GameState) {
        val snapshot = game.snapshot()
        out.putIntArray("board", snapshot.board.map { it?.ordinal ?: -1 }.toIntArray())
        out.putInt("player", snapshot.currentPlayer.ordinal)
        out.putBoolean("finished", snapshot.isFinished)
        out.putInt("winner", snapshot.winner?.ordinal ?: -1)
        out.putIntArray("winning_cells", snapshot.winningCells.map {
            it.row * GameState.COLUMNS + it.column
        }.toIntArray())
    }

    fun restore(saved: Bundle, game: GameState) {
        val board = saved.getIntArray("board") ?: return
        game.restore(GameSnapshot(
            board.map { Player.entries.getOrNull(it) },
            Player.entries[saved.getInt("player")],
            saved.getBoolean("finished"),
            Player.entries.getOrNull(saved.getInt("winner", -1)),
            (saved.getIntArray("winning_cells") ?: intArrayOf()).map {
                Cell(it / GameState.COLUMNS, it % GameState.COLUMNS)
            }
        ))
    }
}

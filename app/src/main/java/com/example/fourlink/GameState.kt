package com.example.fourlink

internal enum class Player {
    YELLOW,
    RED;

    fun opponent(): Player = if (this == YELLOW) RED else YELLOW
}

internal data class Cell(val row: Int, val column: Int)

internal sealed class MoveResult {
    data class Placed(
        val cell: Cell,
        val player: Player,
        val winningCells: List<Cell>,
        val isDraw: Boolean
    ) : MoveResult()

    data object ColumnFull : MoveResult()
    data object GameAlreadyEnded : MoveResult()
}

internal data class GameSnapshot(
    val board: List<Player?>,
    val currentPlayer: Player,
    val isFinished: Boolean,
    val winner: Player?,
    val winningCells: List<Cell>
)

internal class GameState {
    companion object {
        const val ROWS = 6
        const val COLUMNS = 7
    }

    private val board = Array(ROWS) { arrayOfNulls<Player>(COLUMNS) }

    var currentPlayer = Player.YELLOW
        private set

    var isFinished = false
        private set

    var winner: Player? = null
        private set

    var winningCells: List<Cell> = emptyList()
        private set

    fun playerAt(cell: Cell): Player? = board[cell.row][cell.column]

    fun snapshot() = GameSnapshot(board.flatMap { it.toList() }, currentPlayer,
        isFinished, winner, winningCells.toList())

    fun restore(snapshot: GameSnapshot) {
        require(snapshot.board.size == ROWS * COLUMNS)
        snapshot.board.forEachIndexed { index, player -> board[index / COLUMNS][index % COLUMNS] = player }
        currentPlayer = snapshot.currentPlayer
        isFinished = snapshot.isFinished
        winner = snapshot.winner
        winningCells = snapshot.winningCells.toList()
    }

    fun drop(column: Int): MoveResult {
        require(column in 0 until COLUMNS) { "Column must be between 0 and ${COLUMNS - 1}" }
        if (isFinished) return MoveResult.GameAlreadyEnded

        val row = (ROWS - 1 downTo 0).firstOrNull { board[it][column] == null }
            ?: return MoveResult.ColumnFull

        val player = currentPlayer
        board[row][column] = player
        val cell = Cell(row, column)
        val winningCells = winningLine(cell, player)
        val isDraw = winningCells.isEmpty() && board.all { boardRow -> boardRow.all { it != null } }

        if (winningCells.isNotEmpty() || isDraw) {
            isFinished = true
            winner = player.takeIf { winningCells.isNotEmpty() }
            this.winningCells = winningCells
        } else {
            currentPlayer = player.opponent()
        }

        return MoveResult.Placed(cell, player, winningCells, isDraw)
    }

    fun surrender(): Player? {
        if (isFinished) return null
        isFinished = true
        winner = currentPlayer.opponent()
        return winner
    }

    private fun winningLine(cell: Cell, player: Player): List<Cell> {
        // Check both sides of each axis so the last disc can sit anywhere in a four-disc line.
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for ((rowStep, columnStep) in directions) {
            val line = mutableListOf(cell)
            for (sign in listOf(1, -1)) {
                var row = cell.row + rowStep * sign
                var column = cell.column + columnStep * sign
                while (row in 0 until ROWS && column in 0 until COLUMNS && board[row][column] == player) {
                    line.add(Cell(row, column))
                    row += rowStep * sign
                    column += columnStep * sign
                }
            }
            if (line.size >= 4) return line
        }
        return emptyList()
    }
}

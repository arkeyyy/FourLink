package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class GameActivity : TransitionActivity() {
    private val gameState = GameState()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var boardRenderer: GameBoardRenderer
    private var endDialogRunnable: Runnable? = null
    private var isDropAnimating = false
    private var queuedColumn: Int? = null

    override fun onBackPressed() {
        if (gameState.isFinished) super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_screen)
        applyImmersiveMode()

        findViewById<ImageView>(R.id.game_board).visibility = View.INVISIBLE
        findViewById<ImageView>(R.id.back_button).visibility = View.GONE
        findViewById<Button>(R.id.play_again_button).visibility = View.GONE

        boardRenderer = GameBoardRenderer(this)
        boardRenderer.initialize()
        updatePlayerUi()
        bindControls()
    }

    override fun onDestroy() {
        queuedColumn = null
        endDialogRunnable?.let(handler::removeCallbacks)
        endDialogRunnable = null
        if (::boardRenderer.isInitialized) boardRenderer.release()
        super.onDestroy()
    }

    private fun bindControls() {
        val columnButtons = intArrayOf(
            R.id.col_button_1, R.id.col_button_2, R.id.col_button_3,
            R.id.col_button_4, R.id.col_button_5, R.id.col_button_6,
            R.id.col_button_7
        )
        columnButtons.forEachIndexed { column, buttonId ->
            findViewById<Button>(buttonId).setOnClickListener { dropDisc(column) }
        }

        findViewById<Button>(R.id.surrender_button).setOnClickListener {
            if (gameState.isFinished || isDropAnimating) return@setOnClickListener
            val dialog = SurrenderDialogFragment.newInstance(gameState.currentPlayer.name)
            dialog.setOnSurrenderConfirmed {
                val winner = gameState.surrender() ?: return@setOnSurrenderConfirmed
                showGameEnd("PLAYER " + winner.name + " WINS")
            }
            dialog.show(fragmentManager, "SurrenderDialog")
        }

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }
        findViewById<Button>(R.id.play_again_button).setOnClickListener { restartGame() }
    }

    private fun dropDisc(column: Int) {
        if (gameState.isFinished) return
        if (isDropAnimating) {
            if (queuedColumn == null) queuedColumn = column
            return
        }
        findViewById<TextView>(R.id.start_text).visibility = View.GONE

        when (val result = gameState.drop(column)) {
            MoveResult.ColumnFull, MoveResult.GameAlreadyEnded -> Unit
            is MoveResult.Placed -> {
                isDropAnimating = true
                findViewById<Button>(R.id.surrender_button).isEnabled = false
                boardRenderer.showDrop(result.cell, result.player) {
                    if (!isFinishing && !isDestroyed) onChipLanded(result)
                }
            }
        }
    }

    private fun onChipLanded(result: MoveResult.Placed) {
        isDropAnimating = false
        when {
            result.winningCells.isNotEmpty() -> {
                queuedColumn = null
                boardRenderer.blink(result.winningCells, result.player)
                val message = "PLAYER " + result.player.name + " WINS"
                val runnable = Runnable { showGameEnd(message) }
                endDialogRunnable = runnable
                handler.postDelayed(runnable, 2000)
            }
            result.isDraw -> {
                queuedColumn = null
                showGameEnd("Game Draw :(")
            }
            else -> {
                updatePlayerUi()
                val nextColumn = queuedColumn
                queuedColumn = null
                if (nextColumn != null) dropDisc(nextColumn)
            }
        }
        findViewById<Button>(R.id.surrender_button).isEnabled = !isDropAnimating && !gameState.isFinished
    }

    private fun updatePlayerUi() {
        val yellowTab = findViewById<TextView>(R.id.yellow_player_tab)
        val redTab = findViewById<TextView>(R.id.red_player_tab)
        if (gameState.currentPlayer == Player.YELLOW) {
            yellowTab.setBackgroundResource(R.drawable.player_yellow_turn)
            redTab.setBackgroundResource(R.drawable.player_red)
            yellowTab.text = "Yellow's Turn"
            redTab.text = ""
        } else {
            yellowTab.setBackgroundResource(R.drawable.player_yellow)
            redTab.setBackgroundResource(R.drawable.player_red_turn)
            redTab.text = "Red's Turn"
            yellowTab.text = ""
        }
    }

    private fun showGameEnd(message: String) {
        endDialogRunnable = null
        val dialog = GameEndDialogFragment.newInstance(message)
        dialog.setCallbacks(
            onRestart = { restartGame() },
            onMainMenu = {
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }
        )
        dialog.show(fragmentManager, "GameEndDialog")
        findViewById<ImageView>(R.id.back_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.play_again_button).visibility = View.VISIBLE
    }

    private fun restartGame() {
        startActivity(intent)
        finish()
    }
}

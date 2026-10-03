package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback

class GameActivity : TransitionActivity() {
    private val gameState = GameState()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var boardRenderer: GameBoardRenderer
    private var endDialogRunnable: Runnable? = null
    private var isDropAnimating = false
    private var queuedColumn: Int? = null
    private var surrenderAfterLanding = false
    private var resultDueAt = 0L
    private var resultPresented = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            GameSavedState.restore(it, gameState)
            queuedColumn = it.getInt("queued_column", -1).takeIf { column -> column >= 0 }
            surrenderAfterLanding = it.getBoolean("surrender_after_landing")
            resultDueAt = it.getLong("result_due_at")
            resultPresented = it.getBoolean("result_presented")
        }
        setContentView(R.layout.game_screen)
        applyScreenChrome()
        boardRenderer = GameBoardRenderer(this)
        boardRenderer.initialize(::dropDisc)
        // Accepted drops settle on recreation; replaying the animation would accept the move twice.
        boardRenderer.render(gameState)
        updatePlayerUi()
        bindControls()
        onBackPressedDispatcher.addCallback(this) {
            when {
                isDropAnimating -> {
                    queuedColumn = null
                    surrenderAfterLanding = !gameState.isFinished
                }
                gameState.isFinished -> finish()
                else -> requestSurrender()
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (isDropAnimating) return
        if (gameState.isFinished && !resultPresented) {
            if (gameState.winningCells.isNotEmpty()) {
                boardRenderer.blink(gameState.winningCells, requireNotNull(gameState.winner))
                if (resultDueAt == 0L) resultDueAt = System.currentTimeMillis() + 2000
            }
            scheduleResult()
        } else if (surrenderAfterLanding && !gameState.isFinished) {
            surrenderAfterLanding = false
            requestSurrender()
        } else if (!gameState.isFinished) {
            val column = queuedColumn
            queuedColumn = null
            if (column != null) dropDisc(column)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        GameSavedState.write(outState, gameState)
        outState.putInt("queued_column", queuedColumn ?: -1)
        outState.putBoolean("surrender_after_landing", surrenderAfterLanding)
        outState.putLong("result_due_at", resultDueAt)
        outState.putBoolean("result_presented", resultPresented)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        queuedColumn = null
        endDialogRunnable?.let(handler::removeCallbacks)
        boardRenderer.release()
        super.onDestroy()
    }

    private fun bindControls() {
        findViewById<View>(R.id.surrender_button).setOnClickListener { requestSurrender() }
        supportFragmentManager.setFragmentResultListener(SurrenderDialogFragment.RESULT, this) { _, _ ->
            if (gameState.surrender() != null) {
                queuedColumn = null
                updatePlayerUi()
                showGameEnd()
            }
        }
        supportFragmentManager.setFragmentResultListener(GameEndDialogFragment.RESULT, this) { _, result ->
            if (result.getBoolean("restart")) restartGame()
            else {
                startActivity(Intent(this, MainMenuActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }
        }
    }

    private fun requestSurrender() {
        if (gameState.isFinished) return
        queuedColumn = null
        if (isDropAnimating || supportFragmentManager.isStateSaved) {
            surrenderAfterLanding = true
            return
        }
        if (supportFragmentManager.findFragmentByTag("SurrenderDialog") == null) {
            SurrenderDialogFragment.newInstance(gameState.currentPlayer.name)
                .show(supportFragmentManager, "SurrenderDialog")
        }
    }

    private fun dropDisc(column: Int) {
        if (gameState.isFinished || surrenderAfterLanding ||
            supportFragmentManager.findFragmentByTag("SurrenderDialog") != null) return
        if (isDropAnimating) {
            if (queuedColumn == null) queuedColumn = column
            return
        }
        when (val result = gameState.drop(column)) {
            MoveResult.ColumnFull -> findViewById<TextView>(R.id.start_text).setText(R.string.column_full)
            MoveResult.GameAlreadyEnded -> Unit
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
        boardRenderer.describeBoard(gameState)
        updatePlayerUi()
        when {
            result.winningCells.isNotEmpty() -> {
                queuedColumn = null
                surrenderAfterLanding = false
                boardRenderer.blink(result.winningCells, result.player)
                resultDueAt = System.currentTimeMillis() + 2000
                scheduleResult()
            }
            result.isDraw -> {
                queuedColumn = null
                surrenderAfterLanding = false
                showGameEnd()
            }
            surrenderAfterLanding -> {
                surrenderAfterLanding = false
                requestSurrender()
            }
            else -> {
                val nextColumn = queuedColumn
                queuedColumn = null
                if (nextColumn != null) dropDisc(nextColumn)
            }
        }
    }

    private fun updatePlayerUi() {
        val yellow = findViewById<TextView>(R.id.yellow_player_tab)
        val red = findViewById<TextView>(R.id.red_player_tab)
        val yellowActive = !gameState.isFinished && gameState.currentPlayer == Player.YELLOW
        val redActive = !gameState.isFinished && gameState.currentPlayer == Player.RED
        yellow.setText(if (yellowActive) R.string.yellow_turn else R.string.yellow)
        red.setText(if (redActive) R.string.red_turn else R.string.red)
        yellow.setBackgroundResource(if (yellowActive) R.drawable.ui_player_yellow_active else R.drawable.ui_player_yellow)
        red.setBackgroundResource(if (redActive) R.drawable.ui_player_red_active else R.drawable.ui_player_red)
        yellow.isSelected = yellowActive
        red.isSelected = redActive
        findViewById<TextView>(R.id.start_text).setText(
            if (gameState.isFinished) R.string.match_finished else R.string.game_hint)
        findViewById<Button>(R.id.surrender_button).apply {
            isEnabled = !isDropAnimating && !gameState.isFinished
            visibility = if (gameState.isFinished) View.GONE else View.VISIBLE
        }
        if (!gameState.isFinished) (if (yellowActive) yellow else red).announceForAccessibility(
            getString(if (yellowActive) R.string.yellow_turn else R.string.red_turn))
    }

    private fun scheduleResult() {
        endDialogRunnable?.let(handler::removeCallbacks)
        endDialogRunnable = Runnable { showGameEnd() }.also {
            handler.postDelayed(it, (resultDueAt - System.currentTimeMillis()).coerceAtLeast(0))
        }
    }

    private fun showGameEnd() {
        endDialogRunnable = null
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved || resultPresented) return
        if (supportFragmentManager.findFragmentByTag("GameEndDialog") == null) {
            val message = getString(when (gameState.winner) {
                Player.YELLOW -> R.string.yellow_wins
                Player.RED -> R.string.red_wins
                null -> R.string.draw
            })
            GameEndDialogFragment.newInstance(message).show(supportFragmentManager, "GameEndDialog")
        }
        resultPresented = true
    }

    private fun restartGame() {
        startActivity(intent)
        finish()
    }
}

package com.example.fourlink

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.doOnPreDraw
import androidx.core.view.setMargins
import kotlin.math.min

internal class GameBoardRenderer(private val activity: Activity) {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var cellViews: Array<Array<ImageView>>
    private var blinkRunnable: Runnable? = null
    private var dropAnimator: ObjectAnimator? = null
    private var flyingChip: ImageView? = null
    private var pendingLayout: OneShotPreDrawListener? = null
    private var isReleased = false

    fun initialize(onColumnSelected: (Int) -> Unit) {
        rootLayout = activity.findViewById(R.id.root_layout)
        val gridLayout = activity.findViewById<GridLayout>(R.id.game_grid)
        gridLayout.removeAllViews()
        gridLayout.rowCount = GameState.ROWS
        gridLayout.columnCount = GameState.COLUMNS
        (gridLayout as SquareBoard).onColumnSelected = onColumnSelected
        cellViews = Array(GameState.ROWS) { row ->
            Array(GameState.COLUMNS) { column ->
                val cell = ImageView(activity).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(column, 1f)
                        rowSpec = GridLayout.spec(row, 1f)
                        setMargins(0)
                    }
                    setImageResource(R.drawable.ui_chip_empty)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    isFocusable = false
                    setOnClickListener { onColumnSelected(column) }
                }
                gridLayout.addView(cell)
                cell
            }
        }
    }

    fun render(game: GameState) {
        for (row in 0 until GameState.ROWS) for (column in 0 until GameState.COLUMNS) {
            val player = game.playerAt(Cell(row, column))
            cellViews[row][column].setImageResource(player?.let(::discResource) ?: R.drawable.ui_chip_empty)
        }
        describeBoard(game)
    }

    fun describeBoard(game: GameState) {
        val description = StringBuilder(activity.getString(R.string.board_description))
        for (row in 0 until GameState.ROWS) for (column in 0 until GameState.COLUMNS) {
            val player = game.playerAt(Cell(row, column)) ?: continue
            val name = activity.getString(if (player == Player.YELLOW) R.string.yellow else R.string.red)
            description.append(' ').append(activity.getString(R.string.occupied_cell, name, row + 1, column + 1))
        }
        activity.findViewById<GridLayout>(R.id.game_grid).contentDescription = description
    }

    fun showDrop(cell: Cell, player: Player, onLanded: () -> Unit) {
        if (isReleased) return
        check(dropAnimator == null && pendingLayout == null) { "Wait for the previous chip to land" }
        val destination = cellViews[cell.row][cell.column]
        if (!animationsEnabled()) {
            destination.setImageResource(discResource(player))
            onLanded()
            return
        }
        if (destination.isLaidOut && !rootLayout.isLayoutRequested) {
            animateDrop(cell, player, onLanded)
        } else {
            pendingLayout = rootLayout.doOnPreDraw {
                pendingLayout = null
                if (!isReleased) animateDrop(cell, player, onLanded)
            }
        }
    }

    private fun animateDrop(cell: Cell, player: Player, onLanded: () -> Unit) {
        val destination = cellViews[cell.row][cell.column]
        val disc = discResource(player)
        if (!animationsEnabled() || destination.width == 0 || destination.height == 0) {
            destination.setImageResource(disc)
            onLanded()
            return
        }

        val bounds = boundsInRoot(destination)
        val topBounds = boundsInRoot(cellViews[0][cell.column])
        val chip = ImageView(activity).apply {
            setImageResource(disc)
            scaleType = destination.scaleType
            layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val diameter = min(destination.width, destination.height).toFloat()
        val startOffset = topBounds.top - bounds.top - diameter

        // An overlay leaves every board hole in place while the separate chip falls.
        chip.translationY = startOffset
        flyingChip = chip
        rootLayout.overlay.add(chip)

        val animator = ObjectAnimator.ofFloat(chip, View.TRANSLATION_Y, startOffset, 0f).apply {
            duration = DROP_DURATION_MS
            interpolator = BounceInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                private var wasCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    wasCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    dropAnimator = null
                    rootLayout.overlay.remove(chip)
                    flyingChip = null
                    // cancel() also sends onAnimationEnd; it must never advance the game.
                    if (!wasCancelled && !isReleased) {
                        destination.setImageResource(disc)
                        onLanded()
                    }
                }
            })
        }
        dropAnimator = animator
        animator.start()
    }

    private fun boundsInRoot(view: View): Rect = Rect(0, 0, view.width, view.height).also {
        rootLayout.offsetDescendantRectToMyCoords(view, it)
    }

    private fun animationsEnabled(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    fun blink(cells: List<Cell>, player: Player) {
        stopBlinking()
        if (!animationsEnabled()) {
            for (cell in cells) {
                cellViews[cell.row][cell.column].apply {
                    setImageResource(discResource(player))
                    foreground = activity.getDrawable(R.drawable.ui_winner_outline)
                }
            }
            return
        }
        var isVisible = true
        val playerDisc = discResource(player)
        val runnable = object : Runnable {
            override fun run() {
                for (cell in cells) {
                    cellViews[cell.row][cell.column].setImageResource(
                        if (isVisible) R.drawable.ui_chip_empty else playerDisc
                    )
                }
                isVisible = !isVisible
                handler.postDelayed(this, 400)
            }
        }
        blinkRunnable = runnable
        handler.post(runnable)
    }

    fun release() {
        isReleased = true
        pendingLayout?.removeListener()
        pendingLayout = null
        dropAnimator?.cancel()
        dropAnimator = null
        flyingChip?.let { rootLayout.overlay.remove(it) }
        flyingChip = null
        stopBlinking()
    }

    private fun stopBlinking() {
        blinkRunnable?.let(handler::removeCallbacks)
        blinkRunnable = null
    }

    private fun discResource(player: Player): Int = when (player) {
        Player.YELLOW -> R.drawable.ui_chip_yellow
        Player.RED -> R.drawable.ui_chip_red
    }

    private companion object {
        const val DROP_DURATION_MS = 400L
    }
}

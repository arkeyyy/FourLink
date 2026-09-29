package com.example.fourlink

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.doOnPreDraw
import androidx.core.view.setMargins
import kotlin.math.min
import kotlin.math.sqrt

internal class GameBoardRenderer(private val activity: Activity) {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var cellViews: Array<Array<ImageView>>
    private var blinkRunnable: Runnable? = null
    private var dropAnimator: AnimatorSet? = null
    private var flyingChip: ImageView? = null
    private var pendingLayout: OneShotPreDrawListener? = null
    private var isReleased = false

    fun initialize() {
        rootLayout = activity.findViewById(R.id.root_layout)
        val boardImage = ImageView(activity).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.board_full_blue)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            ).apply {
                topToTop = R.id.game_grid
                bottomToBottom = R.id.game_grid
                startToStart = R.id.game_grid
                endToEnd = R.id.game_grid
                topMargin = -25
                bottomMargin = -50
            }
        }
        rootLayout.addView(boardImage, 0)

        val gridLayout = activity.findViewById<GridLayout>(R.id.game_grid)
        gridLayout.removeAllViews()
        gridLayout.rowCount = GameState.ROWS
        gridLayout.columnCount = GameState.COLUMNS
        cellViews = Array(GameState.ROWS) { row ->
            Array(GameState.COLUMNS) { column ->
                val cell = ImageView(activity).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(column, 1f)
                        rowSpec = GridLayout.spec(row, 1f)
                        setMargins(4)
                    }
                    setImageResource(R.drawable.game_piece_empty)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                gridLayout.addView(cell)
                cell
            }
        }
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
        val bottomBounds = boundsInRoot(cellViews[GameState.ROWS - 1][cell.column])
        val chip = ImageView(activity).apply {
            setImageResource(disc)
            scaleType = destination.scaleType
            layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val diameter = min(
            min(destination.width, destination.height),
            min(chip.drawable.intrinsicWidth, chip.drawable.intrinsicHeight)
        ).toFloat()
        val startOffset = topBounds.top - bounds.top - diameter
        val fullDistance = bottomBounds.top - topBounds.top + diameter
        val fallDuration = (MAX_FALL_DURATION_MS * sqrt(-startOffset / fullDistance))
            .toLong().coerceIn(MIN_FALL_DURATION_MS, MAX_FALL_DURATION_MS)
        val rebound = diameter * REBOUND_FRACTION

        // An overlay leaves every board hole in place while the separate chip falls.
        chip.translationY = startOffset
        flyingChip = chip
        rootLayout.overlay.add(chip)

        val fall = ObjectAnimator.ofFloat(chip, View.TRANSLATION_Y, startOffset, 0f).apply {
            duration = fallDuration
            interpolator = AccelerateInterpolator()
        }
        val bounceUp = ObjectAnimator.ofFloat(chip, View.TRANSLATION_Y, 0f, -rebound).apply {
            duration = 8L
            interpolator = DecelerateInterpolator()
        }
        val bounceDown = ObjectAnimator.ofFloat(chip, View.TRANSLATION_Y, -rebound, 0f).apply {
            duration = 12L
            interpolator = AccelerateInterpolator()
        }
        val animator = AnimatorSet().apply {
            playSequentially(fall, bounceUp, bounceDown)
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
        var isVisible = true
        val playerDisc = discResource(player)
        val runnable = object : Runnable {
            override fun run() {
                for (cell in cells) {
                    cellViews[cell.row][cell.column].setImageResource(
                        if (isVisible) R.drawable.game_piece_empty else playerDisc
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
        Player.YELLOW -> R.drawable.game_piece_yellow
        Player.RED -> R.drawable.game_piece_red
    }

    private companion object {
        const val MIN_FALL_DURATION_MS = 23L
        const val MAX_FALL_DURATION_MS = 60L
        const val REBOUND_FRACTION = 0.08f
    }
}

package com.example.fourlink

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.view.KeyEvent
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import kotlin.math.min

class HeadingTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatTextView(context, attrs) {
    init { ViewCompat.setAccessibilityHeading(this, true) }
}

class ContentColumn @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = min(MeasureSpec.getSize(widthMeasureSpec),
            resources.getDimensionPixelSize(R.dimen.content_max_width))
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    }
}

class SquareBoard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    GridLayout(context, attrs) {
    var onColumnSelected: ((Int) -> Unit)? = null
    private var selectedColumn = 0
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * resources.displayMetrics.density
        color = android.graphics.Color.WHITE
    }

    init {
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        for (column in 0 until GameState.COLUMNS) {
            ViewCompat.addAccessibilityAction(this, resources.getString(R.string.drop_column, column + 1)) { _, _ ->
                onColumnSelected?.invoke(column)
                true
            }
        }
        setOnFocusChangeListener { _, _ -> invalidate() }
        setOnKeyListener { _, key, event ->
            when (key) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        val step = if (key == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                        selectedColumn = (selectedColumn + step).coerceIn(0, GameState.COLUMNS - 1)
                        invalidate()
                    }
                    true
                }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (event.action == KeyEvent.ACTION_UP) onColumnSelected?.invoke(selectedColumn)
                    true
                }
                else -> false
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (hasFocus() && !isInTouchMode && childCount == GameState.ROWS * GameState.COLUMNS) {
            val top = getChildAt(selectedColumn)
            val bottom = getChildAt((GameState.ROWS - 1) * GameState.COLUMNS + selectedColumn)
            val inset = selectionPaint.strokeWidth / 2
            canvas.drawRoundRect(top.left + inset, top.top + inset,
                top.right - inset, bottom.bottom - inset, 12f, 12f, selectionPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = min(MeasureSpec.getSize(widthMeasureSpec),
            resources.getDimensionPixelSize(R.dimen.board_max_width))
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val availableHeight = (resources.configuration.screenHeightDp - 160)
                .coerceAtLeast(120) * resources.displayMetrics.density
            width = min(width, (availableHeight * 7 / 6).toInt())
        }
        val height = (width - paddingLeft - paddingRight) * 6 / 7 + paddingTop + paddingBottom
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}

class PlayerPanels @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val gap = (12 * resources.displayMetrics.density).toInt()
    private var arrangedOrientation = -1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val playerWords = (resources.getString(R.string.yellow_turn) + " " +
            resources.getString(R.string.red_turn)).split(' ')
        val minimumPanelWidth = (0 until childCount).maxOfOrNull { index ->
            val label = getChildAt(index) as android.widget.TextView
            val longestWord = playerWords.maxOf { label.paint.measureText(it) }
            longestWord.toInt() + label.paddingLeft + label.paddingRight + 1
        } ?: 0
        val nextOrientation = if (available < minimumPanelWidth * childCount + gap) VERTICAL else HORIZONTAL
        if (arrangedOrientation != nextOrientation) {
            orientation = nextOrientation
            arrangedOrientation = nextOrientation
            for (index in 0 until childCount) {
                getChildAt(index).layoutParams = (getChildAt(index).layoutParams as LayoutParams).apply {
                    width = if (orientation == VERTICAL) LayoutParams.MATCH_PARENT else 0
                    height = if (orientation == VERTICAL) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
                    weight = if (orientation == VERTICAL) 0f else 1f
                    marginStart = if (orientation == HORIZONTAL && index > 0) gap else 0
                    marginEnd = 0
                    topMargin = if (orientation == VERTICAL && index > 0) gap else 0
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

class PatternRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private var arrangedOrientation = -1
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec) / resources.displayMetrics.density
        val stacked = available < 360 || resources.configuration.fontScale >= 1.5f
        val nextOrientation = if (stacked) VERTICAL else HORIZONTAL
        if (arrangedOrientation != nextOrientation) {
            orientation = nextOrientation
            arrangedOrientation = nextOrientation
            for (index in 0 until childCount) {
                getChildAt(index).layoutParams = (getChildAt(index).layoutParams as LayoutParams).apply {
                    width = if (stacked) LayoutParams.MATCH_PARENT else 0
                    weight = if (stacked) 0f else 1f
                    marginEnd = if (!stacked && index == 0) (12 * resources.displayMetrics.density).toInt() else 0
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

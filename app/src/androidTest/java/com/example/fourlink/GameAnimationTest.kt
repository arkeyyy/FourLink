package com.example.fourlink

import android.animation.ValueAnimator
import androidx.fragment.app.DialogFragment
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.core.view.descendants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class GameAnimationTest {
    private lateinit var originalScale: String

    @Test
    fun boardTouchesSelectAllSevenColumns() = withGame { scenario ->
        for (column in 0 until GameState.COLUMNS) {
            scenario.onActivity { activity ->
                val board = activity.findViewById<GridLayout>(R.id.game_grid)
                val target = cell(activity, column % GameState.ROWS, column)
                val x = (target.left + target.right) / 2f
                val y = (target.top + target.bottom) / 2f
                val time = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(time, time, action, x, y, 0)
                    try { assertTrue(board.dispatchTouchEvent(event)) } finally { event.recycle() }
                }
            }
            waitUntil(scenario) { it.findViewById<Button>(R.id.surrender_button).isEnabled }
            scenario.onActivity {
                assertChip(it, 5, column, if (column % 2 == 0) R.drawable.ui_chip_yellow else R.drawable.ui_chip_red)
            }
        }
    }

    @Test
    fun accessibleActionsAndKeyboardDropIntoSelectedColumns() = withGame { scenario ->
        scenario.onActivity { activity ->
            val board = activity.findViewById<SquareBoard>(R.id.game_grid)
            val actions = board.createAccessibilityNodeInfo().actionList
            val columns = actions.filter { action -> (1..7).any {
                action.label?.toString() == activity.getString(R.string.drop_column, it)
            } }
            assertEquals(7, columns.size)
            assertTrue(board.performAccessibilityAction(columns[2].id, null))
        }
        waitUntil(scenario) { it.findViewById<Button>(R.id.surrender_button).isEnabled }
        scenario.onActivity { activity ->
            assertChip(activity, 5, 2, R.drawable.ui_chip_yellow)
            val board = activity.findViewById<SquareBoard>(R.id.game_grid)
            board.requestFocusFromTouch()
            for (key in listOf(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_ENTER)) {
                assertTrue(board.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key)))
                assertTrue(board.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key)))
            }
        }
        waitUntil(scenario) { it.findViewById<Button>(R.id.surrender_button).isEnabled }
        scenario.onActivity { assertChip(it, 5, 1, R.drawable.ui_chip_red) }
    }

    @Before
    fun enableAnimations() {
        originalScale = shell("settings get global animator_duration_scale")
        shell("settings put global animator_duration_scale 1")
    }

    @After
    fun restoreAnimations() {
        shell(if (originalScale == "null") "settings delete global animator_duration_scale"
            else "settings put global animator_duration_scale ${originalScale.toFloat()}")
    }

    @Test
    fun holesStayFixedAndOnlyFirstExtraTapIsQueued() = withGame { scenario ->
        scenario.onActivity { activity ->
            tapColumn(activity, 0)
            tapColumn(activity, 6)
            tapColumn(activity, 3)

            assertChip(activity, 5, 0, R.drawable.ui_chip_empty)
            assertChip(activity, 5, 6, R.drawable.ui_chip_empty)
            assertEquals(0f, cell(activity, 5, 0).translationY, 0f)
            assertEquals("Yellow's turn", activity.findViewById<TextView>(R.id.yellow_player_tab).text)
            assertFalse(activity.findViewById<Button>(R.id.surrender_button).isEnabled)
        }

        waitUntil(scenario) { hasChip(it, 5, 6, R.drawable.ui_chip_red) }
        scenario.onActivity { activity ->
            assertChip(activity, 5, 0, R.drawable.ui_chip_yellow)
            assertChip(activity, 5, 3, R.drawable.ui_chip_empty)
            assertEquals("Yellow's turn", activity.findViewById<TextView>(R.id.yellow_player_tab).text)
            assertTrue(activity.findViewById<Button>(R.id.surrender_button).isEnabled)
        }
    }

    @Test
    fun queuedFullColumnDoesNotConsumeTurn() = withGame { scenario ->
        repeat(GameState.ROWS) { dropAndWait(scenario, 6) }
        scenario.onActivity { activity ->
            assertChip(activity, 0, 6, R.drawable.ui_chip_red)
            assertChip(activity, 3, 6, R.drawable.ui_chip_yellow)
            tapColumn(activity, 0)
            tapColumn(activity, 6)
            tapColumn(activity, 3)
        }

        waitUntil(scenario) { it.findViewById<Button>(R.id.surrender_button).isEnabled }
        scenario.onActivity { activity ->
            assertChip(activity, 5, 0, R.drawable.ui_chip_yellow)
            assertChip(activity, 5, 3, R.drawable.ui_chip_empty)
            assertEquals("Red's turn", activity.findViewById<TextView>(R.id.red_player_tab).text)
        }
        dropAndWait(scenario, 3)
        scenario.onActivity { assertChip(it, 5, 3, R.drawable.ui_chip_red) }
    }

    @Test
    fun winningChipsStayVisibleUntilLastChipLands() = withGame { scenario ->
        listOf(0, 0, 1, 1, 2, 2).forEach { dropAndWait(scenario, it) }
        val startedAt = SystemClock.uptimeMillis()
        scenario.onActivity { activity ->
            tapColumn(activity, 3)
            tapColumn(activity, 6)
            for (column in 0..2) assertChip(activity, 5, column, R.drawable.ui_chip_yellow)
            assertChip(activity, 5, 3, R.drawable.ui_chip_empty)
            assertFalse(endDialogIsShowing(activity))
        }

        waitUntil(scenario) {
            it.findViewById<TextView>(R.id.start_text).text.toString() == it.getString(R.string.match_finished)
        }
        scenario.onActivity { activity ->
            assertFalse(endDialogIsShowing(activity))
            assertFalse(activity.findViewById<ViewGroup>(R.id.root_layout).descendants
                .filterIsInstance<Button>()
                .any { it.visibility == View.VISIBLE && it.text.toString() == activity.getString(R.string.play_again) })
        }
        waitUntil(scenario) { endDialogIsShowing(it) }
        assertTrue(SystemClock.uptimeMillis() - startedAt >= 2000)
        scenario.onActivity { activity ->
            assertEquals("Yellow wins!", endMessage(activity))
            assertChip(activity, 5, 6, R.drawable.ui_chip_empty)
            assertTrue(endDialog(activity)!!.requireDialog().findViewById<View>(R.id.btnRestart).isShown)
        }
    }

    @Test
    fun drawDialogWaitsForLastChip() = withGame { scenario ->
        val sequence = listOf(
            2, 6, 4, 1, 4, 3, 4, 3, 0, 1, 1, 4, 4, 4,
            6, 0, 0, 6, 2, 5, 5, 6, 1, 2, 1, 2, 3, 0,
            3, 6, 0, 3, 0, 3, 6, 2, 5, 5, 2, 1, 5, 5
        )
        sequence.dropLast(1).forEach { dropAndWait(scenario, it) }
        scenario.onActivity { activity ->
            tapColumn(activity, sequence.last())
            assertChip(activity, 0, 5, R.drawable.ui_chip_empty)
            assertFalse(endDialogIsShowing(activity))
        }

        waitUntil(scenario) { endDialogIsShowing(it) }
        scenario.onActivity { activity ->
            assertChip(activity, 0, 5, R.drawable.ui_chip_red)
            assertEquals("It's a draw!", endMessage(activity))
        }
    }

    @Test
    fun releasingRendererDuringFallSuppressesLandingCallback() = withGame { scenario ->
        val landed = AtomicInteger()
        lateinit var renderer: GameBoardRenderer
        scenario.onActivity { activity ->
            renderer = GameBoardRenderer(activity)
            renderer.initialize {}
        }
        waitUntil(scenario) { cell(it, 5, 0).isLaidOut }
        scenario.onActivity {
            renderer.showDrop(Cell(5, 0), Player.YELLOW) { landed.incrementAndGet() }
            renderer.release()
        }
        SystemClock.sleep(650)
        assertEquals(0, landed.get())
        scenario.onActivity { assertChip(it, 5, 0, R.drawable.ui_chip_empty) }
    }

    @Test
    fun releasingRendererBeforeLayoutSuppressesPendingDrop() = withGame { scenario ->
        val landed = AtomicInteger()
        scenario.onActivity { activity ->
            val renderer = GameBoardRenderer(activity)
            renderer.initialize {}
            renderer.showDrop(Cell(5, 0), Player.YELLOW) { landed.incrementAndGet() }
            renderer.release()
        }
        SystemClock.sleep(650)
        assertEquals(0, landed.get())
        scenario.onActivity { assertChip(it, 5, 0, R.drawable.ui_chip_empty) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun disabledAnimationsSettleSynchronously() {
        val originalScale = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 0")
            withGame(animationsEnabled = false) { scenario ->
                waitUntil(scenario) { !ValueAnimator.areAnimatorsEnabled() }
                scenario.onActivity { activity ->
                    tapColumn(activity, 0)
                    assertChip(activity, 5, 0, R.drawable.ui_chip_yellow)
                    assertEquals("Red's turn", activity.findViewById<TextView>(R.id.red_player_tab).text)
                    assertTrue(activity.findViewById<Button>(R.id.surrender_button).isEnabled)
                    tapColumn(activity, 6)
                    assertChip(activity, 5, 6, R.drawable.ui_chip_red)
                }
            }
        } finally {
            if (originalScale == "null") {
                shell("settings delete global animator_duration_scale")
            } else {
                shell("settings put global animator_duration_scale ${originalScale.toFloat()}")
            }
        }
    }

    private fun withGame(animationsEnabled: Boolean = true, block: (ActivityScenario<GameActivity>) -> Unit) {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            waitUntil(scenario) {
                android.os.Build.VERSION.SDK_INT < 26 ||
                    ValueAnimator.areAnimatorsEnabled() == animationsEnabled
            }
            waitUntil(scenario) { cell(it, 5, 0).isLaidOut }
            block(scenario)
        }
    }

    private fun dropAndWait(scenario: ActivityScenario<GameActivity>, column: Int) {
        scenario.onActivity { tapColumn(it, column) }
        waitUntil(scenario) { it.findViewById<Button>(R.id.surrender_button).isEnabled }
    }

    private fun waitUntil(scenario: ActivityScenario<GameActivity>, predicate: (GameActivity) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 6000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            scenario.onActivity { ready = predicate(it) }
            if (ready) return
            SystemClock.sleep(25)
        }
        fail("Animation did not reach the expected state within 6 seconds")
    }

    private fun tapColumn(activity: GameActivity, column: Int) {
        cell(activity, 0, column).performClick()
    }

    private fun cell(activity: GameActivity, row: Int, column: Int): ImageView =
        activity.findViewById<GridLayout>(R.id.game_grid).getChildAt(row * GameState.COLUMNS + column) as ImageView

    private fun hasChip(activity: GameActivity, row: Int, column: Int, resource: Int): Boolean {
        fun pixels(drawable: android.graphics.drawable.Drawable): Bitmap {
            val copy = drawable.constantState!!.newDrawable(activity.resources).mutate()
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).also {
                copy.setBounds(0, 0, 100, 100)
                copy.draw(Canvas(it))
            }
        }
        val actual = pixels(cell(activity, row, column).drawable)
        val expected = pixels(activity.getDrawable(resource)!!)
        return actual.getPixel(50, 50) == expected.getPixel(50, 50) &&
            actual.getPixel(0, 0) == expected.getPixel(0, 0)
    }

    private fun assertChip(activity: GameActivity, row: Int, column: Int, resource: Int) {
        assertTrue("Unexpected chip at row $row, column $column", hasChip(activity, row, column, resource))
    }

    private fun endDialog(activity: GameActivity): DialogFragment? =
        activity.supportFragmentManager.findFragmentByTag("GameEndDialog") as? DialogFragment

    private fun endDialogIsShowing(activity: GameActivity): Boolean = endDialog(activity)?.dialog?.isShowing == true

    private fun endMessage(activity: GameActivity): String =
        endDialog(activity)!!.requireDialog().findViewById<TextView>(R.id.tvMessage).text.toString()

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
    }
}

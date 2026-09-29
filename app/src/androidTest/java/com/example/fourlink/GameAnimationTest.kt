package com.example.fourlink

import android.animation.ValueAnimator
import android.app.DialogFragment
import android.graphics.drawable.BitmapDrawable
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class GameAnimationTest {
    @Test
    fun holesStayFixedAndOnlyFirstExtraTapIsQueued() = withGame { scenario ->
        scenario.onActivity { activity ->
            tapColumn(activity, 0)
            tapColumn(activity, 6)
            tapColumn(activity, 3)

            assertChip(activity, 5, 0, R.drawable.game_piece_empty)
            assertChip(activity, 5, 6, R.drawable.game_piece_empty)
            assertEquals(0f, cell(activity, 5, 0).translationY, 0f)
            assertEquals("Yellow's Turn", activity.findViewById<TextView>(R.id.yellow_player_tab).text)
            assertFalse(activity.findViewById<Button>(R.id.surrender_button).isEnabled)
        }

        waitUntil(scenario) { hasChip(it, 5, 6, R.drawable.game_piece_red) }
        scenario.onActivity { activity ->
            assertChip(activity, 5, 0, R.drawable.game_piece_yellow)
            assertChip(activity, 5, 3, R.drawable.game_piece_empty)
            assertEquals("Yellow's Turn", activity.findViewById<TextView>(R.id.yellow_player_tab).text)
            assertTrue(activity.findViewById<Button>(R.id.surrender_button).isEnabled)
        }
    }

    @Test
    fun queuedFullColumnDoesNotConsumeTurn() = withGame { scenario ->
        repeat(GameState.ROWS) { dropAndWait(scenario, 6) }
        scenario.onActivity { activity ->
            assertChip(activity, 0, 6, R.drawable.game_piece_red)
            assertChip(activity, 3, 6, R.drawable.game_piece_yellow)
            tapColumn(activity, 0)
            tapColumn(activity, 6)
            tapColumn(activity, 3)
        }

        waitUntil(scenario) { it.findViewById<Button>(R.id.surrender_button).isEnabled }
        scenario.onActivity { activity ->
            assertChip(activity, 5, 0, R.drawable.game_piece_yellow)
            assertChip(activity, 5, 3, R.drawable.game_piece_empty)
            assertEquals("Red's Turn", activity.findViewById<TextView>(R.id.red_player_tab).text)
        }
        dropAndWait(scenario, 3)
        scenario.onActivity { assertChip(it, 5, 3, R.drawable.game_piece_red) }
    }

    @Test
    fun winningChipsStayVisibleUntilLastChipLands() = withGame { scenario ->
        listOf(0, 0, 1, 1, 2, 2).forEach { dropAndWait(scenario, it) }
        val startedAt = SystemClock.uptimeMillis()
        scenario.onActivity { activity ->
            tapColumn(activity, 3)
            tapColumn(activity, 6)
            for (column in 0..2) assertChip(activity, 5, column, R.drawable.game_piece_yellow)
            assertChip(activity, 5, 3, R.drawable.game_piece_empty)
            assertFalse(endDialogIsShowing(activity))
        }

        waitUntil(scenario) { endDialogIsShowing(it) }
        assertTrue(SystemClock.uptimeMillis() - startedAt >= 2000)
        scenario.onActivity { activity ->
            assertEquals("PLAYER YELLOW WINS", endMessage(activity))
            assertChip(activity, 5, 6, R.drawable.game_piece_empty)
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
            assertChip(activity, 0, 5, R.drawable.game_piece_empty)
            assertFalse(endDialogIsShowing(activity))
        }

        waitUntil(scenario) { endDialogIsShowing(it) }
        scenario.onActivity { activity ->
            assertChip(activity, 0, 5, R.drawable.game_piece_red)
            assertEquals("Game Draw :(", endMessage(activity))
        }
    }

    @Test
    fun releasingRendererDuringFallSuppressesLandingCallback() = withGame { scenario ->
        val landed = AtomicInteger()
        lateinit var renderer: GameBoardRenderer
        scenario.onActivity { activity ->
            renderer = GameBoardRenderer(activity)
            renderer.initialize()
        }
        waitUntil(scenario) { cell(it, 5, 0).isLaidOut }
        scenario.onActivity {
            renderer.showDrop(Cell(5, 0), Player.YELLOW) { landed.incrementAndGet() }
            renderer.release()
        }
        SystemClock.sleep(650)
        assertEquals(0, landed.get())
        scenario.onActivity { assertChip(it, 5, 0, R.drawable.game_piece_empty) }
    }

    @Test
    fun releasingRendererBeforeLayoutSuppressesPendingDrop() = withGame { scenario ->
        val landed = AtomicInteger()
        scenario.onActivity { activity ->
            val renderer = GameBoardRenderer(activity)
            renderer.initialize()
            renderer.showDrop(Cell(5, 0), Player.YELLOW) { landed.incrementAndGet() }
            renderer.release()
        }
        SystemClock.sleep(650)
        assertEquals(0, landed.get())
        scenario.onActivity { assertChip(it, 5, 0, R.drawable.game_piece_empty) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun disabledAnimationsSettleSynchronously() {
        val originalScale = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 0")
            withGame { scenario ->
                waitUntil(scenario) { !ValueAnimator.areAnimatorsEnabled() }
                scenario.onActivity { activity ->
                    tapColumn(activity, 0)
                    assertChip(activity, 5, 0, R.drawable.game_piece_yellow)
                    assertEquals("Red's Turn", activity.findViewById<TextView>(R.id.red_player_tab).text)
                    assertTrue(activity.findViewById<Button>(R.id.surrender_button).isEnabled)
                    tapColumn(activity, 6)
                    assertChip(activity, 5, 6, R.drawable.game_piece_red)
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

    private fun withGame(block: (ActivityScenario<GameActivity>) -> Unit) {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
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
        val ids = intArrayOf(
            R.id.col_button_1, R.id.col_button_2, R.id.col_button_3,
            R.id.col_button_4, R.id.col_button_5, R.id.col_button_6, R.id.col_button_7
        )
        activity.findViewById<View>(ids[column]).performClick()
    }

    private fun cell(activity: GameActivity, row: Int, column: Int): ImageView =
        activity.findViewById<GridLayout>(R.id.game_grid).getChildAt(row * GameState.COLUMNS + column) as ImageView

    private fun hasChip(activity: GameActivity, row: Int, column: Int, resource: Int): Boolean {
        val actual = (cell(activity, row, column).drawable as BitmapDrawable).bitmap
        val expected = (activity.getDrawable(resource) as BitmapDrawable).bitmap
        return actual.sameAs(expected)
    }

    private fun assertChip(activity: GameActivity, row: Int, column: Int, resource: Int) {
        assertTrue("Unexpected chip at row $row, column $column", hasChip(activity, row, column, resource))
    }

    private fun endDialog(activity: GameActivity): DialogFragment? =
        activity.fragmentManager.findFragmentByTag("GameEndDialog") as? DialogFragment

    private fun endDialogIsShowing(activity: GameActivity): Boolean = endDialog(activity)?.dialog?.isShowing == true

    private fun endMessage(activity: GameActivity): String =
        endDialog(activity)!!.dialog.findViewById<TextView>(R.id.tvMessage).text.toString()

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
    }
}

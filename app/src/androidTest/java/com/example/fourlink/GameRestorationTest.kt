package com.example.fourlink

import android.os.SystemClock
import android.animation.ValueAnimator
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameRestorationTest {
    private lateinit var originalScale: String

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

    private fun withGame(block: (ActivityScenario<GameActivity>) -> Unit) {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            val deadline = SystemClock.uptimeMillis() + 5000
            var ready = false
            while (!ready && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity {
                    ready = android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()
                }
                if (!ready) SystemClock.sleep(25)
            }
            assertTrue("Animation setting did not propagate", ready)
            block(scenario)
        }
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText().trim() }

    @Test
    fun acceptedDropAndQueueSurviveRecreation() {
        withGame { scenario ->
            scenario.onActivity {
                tapColumn(it, 0)
                tapColumn(it, 6)
            }
            scenario.recreate()
            waitUntil(scenario) { it.findViewById<View>(R.id.surrender_button).isEnabled }
            scenario.onActivity {
                val state = snapshot(it)
                assertEquals(Player.YELLOW, state.board[35])
                assertEquals(Player.RED, state.board[41])
                assertEquals(Player.YELLOW, state.currentPlayer)
            }
        }
    }

    @Test
    fun backDuringDropDiscardsQueueAndRestoredDialogsWork() {
        withGame { scenario ->
            scenario.onActivity {
                tapColumn(it, 0)
                tapColumn(it, 6)
                it.onBackPressedDispatcher.onBackPressed()
            }
            waitUntil(scenario) { it.supportFragmentManager.findFragmentByTag("SurrenderDialog") != null }
            scenario.recreate()
            scenario.onActivity {
                val state = snapshot(it)
                assertEquals(Player.YELLOW, state.board[35])
                assertNull(state.board[41])
                val dialog = it.supportFragmentManager.findFragmentByTag("SurrenderDialog") as SurrenderDialogFragment
                dialog.requireDialog().findViewById<View>(R.id.btnNo).performClick()
            }
            SystemClock.sleep(100)
            scenario.onActivity { it.findViewById<View>(R.id.surrender_button).performClick() }
            SystemClock.sleep(100)
            scenario.recreate()
            scenario.onActivity {
                val dialog = it.supportFragmentManager.findFragmentByTag("SurrenderDialog") as SurrenderDialogFragment
                dialog.requireDialog().findViewById<View>(R.id.btnYes).performClick()
            }
            SystemClock.sleep(100)
            scenario.recreate()
            scenario.onActivity {
                val dialog = it.supportFragmentManager.findFragmentByTag("GameEndDialog") as GameEndDialogFragment
                assertEquals(it.getString(R.string.yellow_wins),
                    dialog.requireDialog().findViewById<TextView>(R.id.tvMessage).text.toString())
            }
        }
    }

    @Test
    fun winningMoveKeepsItsResultWhenBackIsPressed() {
        withGame { scenario ->
            listOf(0, 0, 1, 1, 2, 2).forEach { column ->
                scenario.onActivity { tapColumn(it, column) }
                waitUntil(scenario) { it.findViewById<View>(R.id.surrender_button).isEnabled }
            }
            scenario.onActivity {
                tapColumn(it, 3)
                it.onBackPressedDispatcher.onBackPressed()
                assertFalse(it.isFinishing)
            }
            scenario.recreate()
            SystemClock.sleep(2300)
            scenario.onActivity {
                assertNull(it.supportFragmentManager.findFragmentByTag("SurrenderDialog"))
                assertEquals(1, it.supportFragmentManager.fragments.count { fragment -> fragment is GameEndDialogFragment })
                assertEquals(Player.YELLOW, snapshot(it).winner)
            }
        }
    }

    @Test
    fun appearanceSelectionPersistsAndThemeChangesKeepMatch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = Appearance.read(context)
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                val selected = if (original == Appearance.DARK) Appearance.LIGHT else Appearance.DARK
                scenario.onActivity {
                    it.findViewById<View>(if (selected == Appearance.DARK) R.id.theme_dark else R.id.theme_light)
                        .performClick()
                }
                SystemClock.sleep(350)
                scenario.recreate()
                scenario.onActivity {
                    assertEquals(selected, Appearance.read(it))
                    assertEquals(if (selected == Appearance.DARK) R.id.theme_dark else R.id.theme_light,
                        it.findViewById<android.widget.RadioGroup>(R.id.theme_options).checkedRadioButtonId)
                }
            }
            withGame { scenario ->
                scenario.onActivity { tapColumn(it, 0) }
                SystemClock.sleep(150)
                scenario.onActivity { original.apply(it) }
                SystemClock.sleep(350)
                scenario.onActivity {
                    assertEquals(Player.YELLOW, snapshot(it).board[35])
                    assertEquals(Player.RED, snapshot(it).currentPlayer)
                }
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { original.apply(context) }
        }
    }

    private fun tapColumn(activity: GameActivity, column: Int) {
        activity.findViewById<android.widget.GridLayout>(R.id.game_grid).getChildAt(column).performClick()
    }

    private fun waitUntil(scenario: ActivityScenario<GameActivity>, predicate: (GameActivity) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 6000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            scenario.onActivity { ready = predicate(it) }
            if (ready) return
            SystemClock.sleep(25)
        }
        fail("Game did not reach the expected state within 6 seconds")
    }

    private fun snapshot(activity: GameActivity): GameSnapshot {
        val field = GameActivity::class.java.getDeclaredField("gameState").apply { isAccessible = true }
        return (field.get(activity) as GameState).snapshot()
    }
}

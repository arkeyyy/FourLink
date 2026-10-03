package com.example.fourlink

import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class UiLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private data class Device(val name: String, val width: Int, val height: Int,
        val landscape: Boolean, val dark: Boolean, val font: Float)

    @Test
    fun screensFitPhonesTabletsAndLargeText() {
        val context = instrumentation.targetContext
        val previousAppearance = Appearance.read(context)
        val previousFont = shell("settings get system font_scale")
        val previousSize = shell("wm size").substringAfter("Override size: ", "").trim()
        val previousDensity = shell("wm density").substringAfter("Override density: ", "").trim()
        val previousWindowScale = shell("settings get global window_animation_scale")
        val previousTransitionScale = shell("settings get global transition_animation_scale")
        val landingCallback = ActivityLifecycleCallback { activity, stage ->
            if (activity is LandingPageActivity && stage == Stage.RESUMED) {
                val field = LandingPageActivity::class.java.getDeclaredField("navigationHandler")
                field.isAccessible = true
                (field.get(activity) as Handler).removeCallbacksAndMessages(null)
            }
        }
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(landingCallback)
        }
        val devices = listOf(
            Device("phone-light", 360, 640, false, false, 1f),
            Device("phone-large-text", 360, 640, false, true, 2f),
            Device("phone-landscape", 360, 640, true, true, 2f),
            Device("large-phone", 448, 997, false, true, 1f),
            Device("tablet", 800, 1280, false, false, 1f),
            Device("tablet-landscape", 800, 1280, true, true, 1f)
        )
        try {
            shell("settings put global window_animation_scale 0")
            shell("settings put global transition_animation_scale 0")
            val requestedDevice = InstrumentationRegistry.getArguments().getString("device")
            for (device in devices.filter { requestedDevice == null || it.name == requestedDevice }) {
                shell("wm size ${device.width}x${device.height}")
                shell("wm density 160")
                shell("settings put system font_scale ${device.font}")
                instrumentation.runOnMainSync {
                    (if (device.dark) Appearance.DARK else Appearance.LIGHT).apply(context)
                }
                SystemClock.sleep(350)
                val activities = listOf(LandingPageActivity::class.java, MainMenuActivity::class.java,
                    TutorialActivity::class.java, SettingsActivity::class.java,
                    DeveloperPageActivity::class.java, GameActivity::class.java)
                for (activity in activities) {
                    ActivityScenario.launch(activity).use { scenario ->
                        scenario.onActivity {
                            if (it is LandingPageActivity) {
                                val field = LandingPageActivity::class.java.getDeclaredField("navigationHandler")
                                field.isAccessible = true
                                (field.get(it) as Handler).removeCallbacksAndMessages(null)
                            }
                            it.requestedOrientation = if (device.landscape)
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        SystemClock.sleep(500)
                        instrumentation.waitForIdleSync()
                        scenario.onActivity {
                            if (it is LandingPageActivity) {
                                val field = LandingPageActivity::class.java.getDeclaredField("navigationHandler")
                                field.isAccessible = true
                                (field.get(it) as Handler).removeCallbacksAndMessages(null)
                            }
                            checkViews(it.window.decorView, device.name + "/" + activity.simpleName)
                            if (it is GameActivity) {
                                val grid = it.findViewById<GridLayout>(R.id.game_grid)
                                assertEquals(42, grid.childCount)
                                assertTrue(abs(grid.getChildAt(0).width - grid.getChildAt(0).height) <= 1)
                            }
                        }
                        capture(device.name + "-" + activity.simpleName)
                        if (activity == GameActivity::class.java) {
                            scenario.onActivity { it.findViewById<View>(R.id.surrender_button).performClick() }
                            SystemClock.sleep(200)
                            scenario.onActivity {
                                val dialog = (it as GameActivity).supportFragmentManager
                                    .findFragmentByTag("SurrenderDialog") as SurrenderDialogFragment
                                checkViews(dialog.requireDialog().window!!.decorView, device.name + "/surrender")
                            }
                            capture(device.name + "-surrender")
                            scenario.onActivity {
                                val dialog = (it as GameActivity).supportFragmentManager
                                    .findFragmentByTag("SurrenderDialog") as SurrenderDialogFragment
                                dialog.requireDialog().findViewById<View>(R.id.btnYes).performClick()
                            }
                            SystemClock.sleep(200)
                            scenario.onActivity {
                                val dialog = (it as GameActivity).supportFragmentManager
                                    .findFragmentByTag("GameEndDialog") as GameEndDialogFragment
                                checkViews(dialog.requireDialog().window!!.decorView, device.name + "/result")
                            }
                            capture(device.name + "-result")
                        }
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(landingCallback)
            }
            shell(if (previousSize.isBlank()) "wm size reset" else "wm size $previousSize")
            shell(if (previousDensity.isBlank()) "wm density reset" else "wm density $previousDensity")
            shell(if (previousFont == "null") "settings delete system font_scale" else
                "settings put system font_scale $previousFont")
            instrumentation.runOnMainSync { previousAppearance.apply(context) }
            for ((key, value) in listOf("window_animation_scale" to previousWindowScale,
                "transition_animation_scale" to previousTransitionScale)) {
                shell(if (value == "null") "settings delete global $key" else "settings put global $key $value")
            }
        }
    }

    private fun checkViews(view: View, screen: String) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            val label = "$screen: ${view.text}"
            val layout = view.layout
            assertNotNull(label, layout)
            if (layout != null) {
                for (line in 0 until layout.lineCount) assertEquals(label, 0, layout.getEllipsisCount(line))
                assertTrue("$label clips vertically", layout.height <= view.height -
                    view.compoundPaddingTop - view.compoundPaddingBottom + 2)
            }
        }
        if (view is Button) {
            val min = 48 * view.resources.displayMetrics.density - 1
            assertTrue("$screen: ${view.text} target too small", view.width >= min && view.height >= min)
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) checkViews(view.getChildAt(i), screen)
    }

    private fun capture(name: String) {
        shell("mkdir -p /sdcard/Download/fourlink-modern")
        shell("screencap -p /sdcard/Download/fourlink-modern/$name.png")
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText().trim() }
}

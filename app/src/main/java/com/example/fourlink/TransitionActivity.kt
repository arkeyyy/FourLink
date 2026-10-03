package com.example.fourlink

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar

open class TransitionActivity : AppCompatActivity() {
    protected fun applyScreenChrome() {
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val landing = this is LandingPageActivity
        val backgroundColor = when (this) {
            is LandingPageActivity -> R.color.ui_art_background
            is TutorialActivity -> R.color.ui_tutorial_canvas
            else -> R.color.ui_canvas
        }
        window.statusBarColor = ContextCompat.getColor(this, backgroundColor)
        window.navigationBarColor = window.statusBarColor
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark || landing
            isAppearanceLightNavigationBars = !dark || landing
            if (this@TransitionActivity is GameActivity) {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
        findViewById<MaterialToolbar?>(R.id.page_toolbar)?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

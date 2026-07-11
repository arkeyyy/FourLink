package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View

class LandingPageActivity : TransitionActivity() {
    private val navigationHandler = Handler(Looper.getMainLooper())
    private val navigateToMainMenu = Runnable {
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landing_page_screen)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        navigationHandler.postDelayed(navigateToMainMenu, 3000L)
    }

    override fun onDestroy() {
        navigationHandler.removeCallbacks(navigateToMainMenu)
        super.onDestroy()
    }
}

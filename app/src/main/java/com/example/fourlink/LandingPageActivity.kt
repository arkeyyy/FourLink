package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class LandingPageActivity : TransitionActivity() {
    private val navigationHandler = Handler(Looper.getMainLooper())
    private val navigateToMainMenu = Runnable {
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landing_page_screen)
        applyScreenChrome()

        navigationHandler.postDelayed(navigateToMainMenu, 3000L)
    }

    override fun onDestroy() {
        navigationHandler.removeCallbacks(navigateToMainMenu)
        super.onDestroy()
    }
}

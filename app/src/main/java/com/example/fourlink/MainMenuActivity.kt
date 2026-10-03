package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.view.View

class MainMenuActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)
        applyScreenChrome()
        findViewById<View>(R.id.play_button).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        findViewById<View>(R.id.tutorial_button).setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }
        findViewById<View>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}

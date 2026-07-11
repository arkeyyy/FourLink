package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView


class MainMenuActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        val play_button = findViewById<ImageView>(R.id.play_button)
        val tutorial_button = findViewById<Button>(R.id.tutorial_button)
        val settings_button = findViewById<Button>(R.id.settings_button)

        settings_button.setOnClickListener{
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        tutorial_button.setOnClickListener{
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        play_button.setOnClickListener{
            startActivity(Intent(this, GameActivity::class.java))
        }
    }
}

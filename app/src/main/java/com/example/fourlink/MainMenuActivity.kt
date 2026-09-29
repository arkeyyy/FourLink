package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView


class MainMenuActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)
        applyImmersiveMode()

        val playButton = findViewById<ImageView>(R.id.play_button)
        val tutorialButton = findViewById<Button>(R.id.tutorial_button)
        val settingsButton = findViewById<Button>(R.id.settings_button)

        settingsButton.setOnClickListener{
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        tutorialButton.setOnClickListener{
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        playButton.setOnClickListener{
            startActivity(Intent(this, GameActivity::class.java))
        }
    }
}

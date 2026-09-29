package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class SettingsActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_screen)
        applyImmersiveMode()

        val backButton = findViewById<ImageView>(R.id.back_button)
        val developerButton = findViewById<TextView>(R.id.dev_bar)

        backButton.setOnClickListener{
            finish()
        }

        developerButton.setOnClickListener{
            startActivity(Intent(this, DeveloperPageActivity::class.java))
        }

    }
}

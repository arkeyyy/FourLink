package com.example.fourlink

import android.os.Bundle
import android.widget.Button

class TutorialActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tutorial_screen)
        applyImmersiveMode()

        val backButton = findViewById<Button>(R.id.back_button)

        backButton.setOnClickListener{
            finish()
        }
    }
}

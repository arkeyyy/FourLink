package com.example.fourlink

import android.os.Bundle
import android.widget.ImageView

class DeveloperPageActivity: TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.developer_page_screen)
        applyImmersiveMode()

        val backButton = findViewById<ImageView>(R.id.back_button)

        backButton.setOnClickListener{
            finish()
        }
    }
}

package com.example.fourlink

import android.os.Bundle

class TutorialActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tutorial_screen)
        applyScreenChrome()
    }
}

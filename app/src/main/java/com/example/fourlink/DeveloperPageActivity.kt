package com.example.fourlink

import android.os.Bundle

class DeveloperPageActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.developer_page_screen)
        applyScreenChrome()
    }
}

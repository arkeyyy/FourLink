package com.example.fourlink

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView

class SettingsActivity : TransitionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_screen)
        applyScreenChrome()
        val options = mapOf(
            R.id.theme_system to Appearance.SYSTEM,
            R.id.theme_light to Appearance.LIGHT,
            R.id.theme_dark to Appearance.DARK
        )
        findViewById<RadioGroup>(R.id.theme_options).apply {
            // Preferences must win over the old radio state after a theme recreation.
            isSaveFromParentEnabled = false
            check(options.entries.first { it.value == Appearance.read(this@SettingsActivity) }.key)
            setOnCheckedChangeListener { _, id -> options[id]?.apply(this@SettingsActivity) }
        }
        findViewById<View>(R.id.dev_bar).setOnClickListener {
            startActivity(Intent(this, DeveloperPageActivity::class.java))
        }
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.app_version).text = getString(R.string.version, version)
    }
}

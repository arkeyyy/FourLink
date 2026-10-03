package com.example.fourlink

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

internal enum class Appearance(val nightMode: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    fun apply(context: Context) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
            .edit().putString("mode", name).apply()
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    companion object {
        fun read(context: Context): Appearance {
            val name = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                .getString("mode", SYSTEM.name)
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }
}

class FourLinkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Appearance.read(this).nightMode)
    }
}

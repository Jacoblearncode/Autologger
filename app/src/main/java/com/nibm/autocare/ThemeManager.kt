package com.nibm.autocare

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS = "theme_prefs"
    private const val KEY_MODE = "theme_mode"
    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"
    const val MODE_SYSTEM = "system"

    fun applyTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(toDelegate(getMode(context)))
    }

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(toDelegate(mode))
    }

    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, MODE_LIGHT) ?: MODE_LIGHT

    private fun toDelegate(mode: String) = when (mode) {
        MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        MODE_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_NO
    }
}

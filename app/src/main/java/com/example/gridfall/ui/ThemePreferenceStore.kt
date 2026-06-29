package com.example.gridfall.ui

import android.content.Context
import com.example.gridfall.ui.theme.GridfallThemeMode

object ThemePreferenceStore {
    private const val PREFS_NAME = "gridfall_theme"
    private const val KEY_THEME_MODE = "theme_mode"

    fun load(context: Context): GridfallThemeMode {
        val id = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)

        return GridfallThemeMode.fromId(id)
    }

    fun save(context: Context, mode: GridfallThemeMode) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.id)
            .apply()
    }
}

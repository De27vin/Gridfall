package com.example.gridfall.ui

import android.content.Context
import com.example.gridfall.game.GridLayoutPreset

object GridLayoutPreferenceStore {
    private const val PREFS_NAME = "gridfall_grid_layout"
    private const val KEY_GRID_LAYOUT = "grid_layout"

    fun load(context: Context): GridLayoutPreset {
        val id = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GRID_LAYOUT, null)
        return GridLayoutPreset.fromId(id)
    }

    fun save(context: Context, preset: GridLayoutPreset) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GRID_LAYOUT, preset.id)
            .apply()
    }
}

package com.example.gridfall.audio

import android.content.Context

object SoundPreferenceStore {
    private const val PREFS_NAME = "gridfall_sound"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    fun load(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun save(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
    }
}

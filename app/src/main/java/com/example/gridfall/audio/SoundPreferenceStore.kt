package com.example.gridfall.audio

import android.content.Context

object SoundPreferenceStore {
    private const val PREFS_NAME = "gridfall_sound"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_SOUND_EFFECTS_VOLUME = "sound_effects_volume"
    private const val KEY_BACKGROUND_MUSIC_VOLUME = "background_music_volume"

    fun loadSoundEffectsVolume(context: Context): Float {
        return loadVolume(context, KEY_SOUND_EFFECTS_VOLUME)
    }

    fun saveSoundEffectsVolume(context: Context, volume: Float) {
        saveVolume(context, KEY_SOUND_EFFECTS_VOLUME, volume)
    }

    fun loadBackgroundMusicVolume(context: Context): Float {
        return loadVolume(context, KEY_BACKGROUND_MUSIC_VOLUME)
    }

    fun saveBackgroundMusicVolume(context: Context, volume: Float) {
        saveVolume(context, KEY_BACKGROUND_MUSIC_VOLUME, volume)
    }

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

    private fun loadVolume(context: Context, key: String): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(key)) {
            return prefs.getFloat(key, 1.0f).coerceIn(0f, 1f)
        }

        return if (prefs.getBoolean(KEY_SOUND_ENABLED, true)) 1.0f else 0f
    }

    private fun saveVolume(context: Context, key: String, volume: Float) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(key, volume.coerceIn(0f, 1f))
            .apply()
    }
}

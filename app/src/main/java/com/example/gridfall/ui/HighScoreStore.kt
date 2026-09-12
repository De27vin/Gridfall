package com.example.gridfall.ui

import android.content.Context
import com.example.gridfall.game.GridLayoutPreset

object HighScoreStore {
    private const val PREFS_NAME = "gridfall_high_score"
    private const val LEGACY_KEY_HIGH_SCORE = "high_score"

    fun load(context: Context, gridLayout: GridLayoutPreset): Int {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = keyFor(gridLayout)
        if (preferences.contains(key)) return preferences.getInt(key, 0)

        if (gridLayout == GridLayoutPreset.Classic && preferences.contains(LEGACY_KEY_HIGH_SCORE)) {
            val legacyHighScore = preferences.getInt(LEGACY_KEY_HIGH_SCORE, 0)
            preferences.edit().putInt(key, legacyHighScore).apply()
            return legacyHighScore
        }
        return 0
    }

    fun save(context: Context, gridLayout: GridLayoutPreset, highScore: Int) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(keyFor(gridLayout), highScore.coerceAtLeast(0))
            .apply()
    }

    fun clear(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(LEGACY_KEY_HIGH_SCORE)
            .also { editor -> GridLayoutPreset.entries.forEach { editor.remove(keyFor(it)) } }
            .apply()
    }

    internal fun keyFor(gridLayout: GridLayoutPreset): String = "high_score_${gridLayout.id}"

    fun accountBestScore(backendBestScore: Int): Int {
        return backendBestScore.coerceAtLeast(0)
    }
}

package com.example.gridfall.ui

import android.content.Context

object HighScoreStore {
    private const val PREFS_NAME = "gridfall_high_score"
    private const val KEY_HIGH_SCORE = "high_score"

    fun load(context: Context): Int {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HIGH_SCORE, 0)
    }

    fun save(context: Context, highScore: Int) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HIGH_SCORE, highScore)
            .apply()
    }
}

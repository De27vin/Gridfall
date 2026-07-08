package com.example.gridfall.auth

import android.content.Context

object AuthPromptStore {
    private const val PREFS_NAME = "gridfall_auth_prompts"
    private const val KEY_SAVE_PROMPT_DISMISSED = "auth_save_prompt_dismissed"

    fun isSavePromptDismissed(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SAVE_PROMPT_DISMISSED, false)
    }

    fun setSavePromptDismissed(
        context: Context,
        dismissed: Boolean = true
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAVE_PROMPT_DISMISSED, dismissed)
            .apply()
    }
}


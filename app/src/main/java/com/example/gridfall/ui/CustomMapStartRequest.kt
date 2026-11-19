package com.example.gridfall.ui

import com.example.gridfall.game.CustomMapDesign

/** Retain the requested navigation destination while a progress-reset confirmation is open. */
internal data class CustomMapStartRequest(val design: CustomMapDesign, val keepSettingsOpen: Boolean)

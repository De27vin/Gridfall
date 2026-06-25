package com.example.gridfall.ui

import androidx.compose.ui.geometry.Offset

data class BoardLayoutInfo(
    val topLeft: Offset,
    val sizePx: Float,
    val cellSizePx: Float,
    val spacingPx: Float
)

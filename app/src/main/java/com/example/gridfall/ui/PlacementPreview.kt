package com.example.gridfall.ui

import com.example.gridfall.game.Piece

data class PlacementPreview(
    val piece: Piece,
    val originRow: Int,
    val originCol: Int,
    val isValid: Boolean
)

package com.example.gridfall.ui

import androidx.compose.ui.geometry.Offset
import com.example.gridfall.game.Piece

data class DragState(
    val pieceIndex: Int? = null,
    val piece: Piece? = null,
    val dragPosition: Offset = Offset.Zero,
    val dragStartOffset: Offset = Offset.Zero,
    val isDragging: Boolean = false
)

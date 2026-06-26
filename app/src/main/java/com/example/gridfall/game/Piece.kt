package com.example.gridfall.game

data class Piece(
    val id: String,
    val cells: List<Cell>,
    val effect: PieceEffect = PieceEffect.Normal
)

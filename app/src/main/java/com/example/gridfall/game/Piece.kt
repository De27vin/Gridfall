package com.example.gridfall.game

data class Piece(
    val id: String,
    val cells: List<Cell>,
    val effect: PieceEffect = PieceEffect.Normal,
    val rarity: PieceRarity = PieceRarity.Common
) {
    val width: Int
        get() = if (cells.isEmpty()) 0 else cells.maxOf { it.col } - cells.minOf { it.col } + 1

    val height: Int
        get() = if (cells.isEmpty()) 0 else cells.maxOf { it.row } - cells.minOf { it.row } + 1

    val cellCount: Int
        get() = cells.size
}

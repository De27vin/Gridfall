package com.example.gridfall.game

data class GameState(
    val board: Board,
    val currentPieces: List<Piece>,
    val usedPieceIds: Set<String>,
    val score: Int,
    val combo: Int,
    val isGameOver: Boolean
)

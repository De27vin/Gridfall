package com.example.gridfall.game

data class GameState(
    val board: Board,
    val currentPieces: List<Piece>,
    val usedPieceIndices: Set<Int>,
    val score: Int,
    val combo: Int,
    val isGameOver: Boolean,
    val contractState: ContractState = ContractState()
)

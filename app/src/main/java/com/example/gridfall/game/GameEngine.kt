package com.example.gridfall.game

object GameEngine {
    fun createInitialState(): GameState {
        return GameState(
            board = Board.empty(),
            currentPieces = PieceGenerator.generateBatch(),
            usedPieceIds = emptySet(),
            score = 0,
            combo = 0,
            isGameOver = false
        )
    }
}

package com.example.gridfall.game

import kotlin.random.Random

object PieceGenerator {
    fun generateBatch(
        count: Int = 3,
        availablePieces: List<Piece> = PieceLibrary.starterPieces
    ): List<Piece> {
        return List(count) {
            availablePieces.random(Random.Default)
        }
    }
}

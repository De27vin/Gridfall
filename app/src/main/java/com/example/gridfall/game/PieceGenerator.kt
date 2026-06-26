package com.example.gridfall.game

import kotlin.random.Random

object PieceGenerator {
    private const val BOMB_CHANCE_PER_BATCH = 0.10f

    fun generateBatch(
        count: Int = 3,
        availablePieces: List<Piece> = PieceLibrary.starterPieces,
        bombPiece: Piece = PieceLibrary.bombPiece,
        bombChance: Float = BOMB_CHANCE_PER_BATCH,
        random: Random = Random.Default
    ): List<Piece> {
        if (count <= 0) return emptyList()
        val normalPieces = availablePieces.filter { it.effect != PieceEffect.Bomb }
        if (normalPieces.isEmpty()) return emptyList()

        val batch = List(count) {
            normalPieces.random(random)
        }.toMutableList()

        if (random.nextFloat() < bombChance) {
            val bombIndex = random.nextInt(count)
            batch[bombIndex] = bombPiece
        }

        return batch
    }
}

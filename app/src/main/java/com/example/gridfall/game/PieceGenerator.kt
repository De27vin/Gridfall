package com.example.gridfall.game

import kotlin.random.Random

object PieceGenerator {
    private const val BOMB_CHANCE_PER_BATCH = 0.10f
    private const val MAX_LEVEL_BOMB_CHANCE = 0.18f

    fun generateBatch(
        count: Int = 3,
        level: Int = 1,
        availablePieces: List<Piece> = PieceLibrary.starterPieces,
        bombPiece: Piece = PieceLibrary.bombPiece,
        bombChance: Float = BOMB_CHANCE_PER_BATCH,
        random: Random = Random.Default
    ): List<Piece> {
        if (count <= 0) return emptyList()
        val normalPieces = availablePieces.filter { it.effect != PieceEffect.Bomb }
        if (normalPieces.isEmpty()) return emptyList()

        val weightedPieces = weightedPiecesForLevel(
            pieces = normalPieces,
            level = level
        )
        val batch = List(count) {
            weightedPieces.random(random)
        }.toMutableList()

        if (random.nextFloat() < effectiveBombChance(level, bombChance)) {
            val bombIndex = random.nextInt(count)
            batch[bombIndex] = bombPiece
        }

        return batch
    }

    private fun weightedPiecesForLevel(
        pieces: List<Piece>,
        level: Int
    ): List<Piece> {
        val smallPieces = pieces.filter { it.cells.size <= 2 }
        val mediumPieces = pieces.filter { it.cells.size == 3 }
        val largePieces = pieces.filter { it.cells.size >= 4 }

        return when {
            level <= 2 -> pieces +
                smallPieces +
                smallPieces +
                mediumPieces

            level <= 5 -> pieces

            else -> pieces +
                mediumPieces +
                largePieces +
                largePieces
        }.ifEmpty { pieces }
    }

    private fun effectiveBombChance(
        level: Int,
        bombChance: Float
    ): Float {
        if (bombChance != BOMB_CHANCE_PER_BATCH) {
            return bombChance.coerceIn(0f, 1f)
        }

        return (bombChance + (level.coerceAtLeast(1) - 1) * 0.015f)
            .coerceAtMost(MAX_LEVEL_BOMB_CHANCE)
    }
}

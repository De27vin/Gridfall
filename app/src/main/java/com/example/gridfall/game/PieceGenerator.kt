package com.example.gridfall.game

import kotlin.random.Random

object PieceGenerator {
    private const val DEFAULT_BOMB_CHANCE_PER_BATCH = -1f

    fun generateBatch(
        count: Int = 3,
        level: Int = 1,
        availablePieces: List<Piece> = PieceLibrary.starterPieces,
        bombPiece: Piece = PieceLibrary.bombPiece,
        bombChance: Float = DEFAULT_BOMB_CHANCE_PER_BATCH,
        random: Random = Random.Default
    ): List<Piece> {
        if (count <= 0) return emptyList()
        val normalPieces = availablePieces.filter { it.effect != PieceEffect.Bomb }
        if (normalPieces.isEmpty()) return emptyList()

        val batch = List(count) {
            normalPieces.weightedRandom(level, random)
        }.toMutableList()

        if (random.nextFloat() < effectiveBombChance(level, bombChance)) {
            val bombIndex = random.nextInt(count)
            batch[bombIndex] = bombPiece
        }

        return batch
    }

    private fun List<Piece>.weightedRandom(
        level: Int,
        random: Random
    ): Piece {
        val weightedPieces = map { piece ->
            piece to rarityWeight(piece.rarity, level)
        }.filter { (_, weight) ->
            weight > 0
        }

        if (weightedPieces.isEmpty()) return this[random.nextInt(size)]

        val totalWeight = weightedPieces.sumOf { (_, weight) -> weight }
        var target = random.nextInt(totalWeight)
        weightedPieces.forEach { (piece, weight) ->
            if (target < weight) return piece
            target -= weight
        }

        return weightedPieces.last().first
    }

    internal fun rarityWeight(
        rarity: PieceRarity,
        level: Int
    ): Int {
        return when {
            level <= 2 -> when (rarity) {
                PieceRarity.Common -> 75
                PieceRarity.Uncommon -> 22
                PieceRarity.Rare -> 3
                PieceRarity.Epic -> 0
            }

            level <= 5 -> when (rarity) {
                PieceRarity.Common -> 60
                PieceRarity.Uncommon -> 28
                PieceRarity.Rare -> 10
                PieceRarity.Epic -> 2
            }

            else -> when (rarity) {
                PieceRarity.Common -> 48
                PieceRarity.Uncommon -> 32
                PieceRarity.Rare -> 15
                PieceRarity.Epic -> 5
            }
        }
    }

    private fun effectiveBombChance(
        level: Int,
        bombChance: Float
    ): Float {
        if (bombChance != DEFAULT_BOMB_CHANCE_PER_BATCH) {
            return bombChance.coerceIn(0f, 1f)
        }

        return when {
            level <= 2 -> 0.05f
            level <= 5 -> 0.08f
            else -> 0.10f
        }
    }
}

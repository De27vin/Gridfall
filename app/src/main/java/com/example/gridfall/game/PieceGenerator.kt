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
        val normalPieces = availablePieces.filter { it.effect == PieceEffect.Normal }
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
        return rarityWeightsForLevel(level).getValue(rarity)
    }

    internal fun rarityWeightsForLevel(level: Int): Map<PieceRarity, Int> {
        return when {
            level <= 2 -> mapOf(
                PieceRarity.Common to 75,
                PieceRarity.Uncommon to 22,
                PieceRarity.Rare to 3,
                PieceRarity.Epic to 0
            )

            level <= 5 -> mapOf(
                PieceRarity.Common to 60,
                PieceRarity.Uncommon to 28,
                PieceRarity.Rare to 10,
                PieceRarity.Epic to 2
            )

            level <= 7 -> mapOf(
                PieceRarity.Common to 48,
                PieceRarity.Uncommon to 32,
                PieceRarity.Rare to 15,
                PieceRarity.Epic to 5
            )

            else -> {
                val extraLevels = (level - 8).coerceAtLeast(0)
                mapOf(
                    PieceRarity.Common to (45 - (extraLevels * 5 / 2)).coerceAtLeast(25),
                    PieceRarity.Uncommon to (32 - (extraLevels * 2 / 7)).coerceIn(28, 32),
                    PieceRarity.Rare to (17 + (extraLevels * 3 / 2)).coerceAtMost(30),
                    PieceRarity.Epic to (6 + (extraLevels * 9 / 7)).coerceAtMost(17)
                )
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

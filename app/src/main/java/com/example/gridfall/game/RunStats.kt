package com.example.gridfall.game

import java.util.UUID

data class RunStats(
    val runId: String = UUID.randomUUID().toString(),
    val startedAtEpochMillis: Long = System.currentTimeMillis(),
    val linesCleared: Int = 0,
    val contractsCompleted: Int = 0,
    val bombsUsed: Int = 0,
    val megaBombsUsed: Int = 0
) {
    fun durationSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        return ((nowMillis - startedAtEpochMillis) / 1000L)
            .coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    fun recordPlacement(
        pieceEffect: PieceEffect,
        clearedLineCount: Int
    ): RunStats {
        return copy(
            linesCleared = linesCleared + clearedLineCount,
            bombsUsed = bombsUsed + if (pieceEffect == PieceEffect.Bomb) 1 else 0,
            megaBombsUsed = megaBombsUsed + if (pieceEffect == PieceEffect.MegaBomb) 1 else 0
        )
    }

    fun recordCompletedContract(): RunStats {
        return copy(contractsCompleted = contractsCompleted + 1)
    }

    companion object {
        fun newRun(nowMillis: Long = System.currentTimeMillis()): RunStats {
            return RunStats(startedAtEpochMillis = nowMillis)
        }
    }
}

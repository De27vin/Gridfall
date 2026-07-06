package com.example.gridfall.game

object LevelSystem {
    private val fixedLevelThresholds = mapOf(
        1 to 0,
        2 to 60,
        3 to 150,
        4 to 300,
        5 to 550,
        6 to 900,
        7 to 1400,
        8 to 2000
    )

    fun levelForScore(score: Int): Int {
        val safeScore = score.coerceAtLeast(0).toLong()
        var level = 1

        while (thresholdForLevelLong(level + 1) <= safeScore) {
            level += 1
        }

        return level
    }

    fun nextLevelScore(currentLevel: Int): Int {
        return thresholdForLevel(currentLevel + 1)
    }

    fun thresholdForLevel(level: Int): Int {
        return thresholdForLevelLong(level).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun thresholdForLevelLong(level: Int): Long {
        if (level <= 1) return 0
        fixedLevelThresholds[level]?.let { return it.toLong() }

        val extraLevels = (level - 8).toLong()
        val addedScore = extraLevels * 600 + 150 * extraLevels * (extraLevels + 1) / 2
        return fixedLevelThresholds.getValue(8) + addedScore
    }
}

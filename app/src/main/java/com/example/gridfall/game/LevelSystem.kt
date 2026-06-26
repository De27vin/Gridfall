package com.example.gridfall.game

object LevelSystem {
    fun levelForScore(score: Int): Int {
        return when {
            score >= 2000 -> 8
            score >= 1400 -> 7
            score >= 900 -> 6
            score >= 550 -> 5
            score >= 300 -> 4
            score >= 150 -> 3
            score >= 60 -> 2
            else -> 1
        }
    }

    fun nextLevelScore(currentLevel: Int): Int? {
        return when (currentLevel) {
            1 -> 60
            2 -> 150
            3 -> 300
            4 -> 550
            5 -> 900
            6 -> 1400
            7 -> 2000
            else -> null
        }
    }
}

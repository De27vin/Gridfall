package com.example.gridfall.game

object ScoreSystem {
    const val POINTS_PER_CELL = 1
    const val POINTS_PER_LINE = 10
    const val MULTI_LINE_BONUS_PER_EXTRA_LINE = 20
    const val CROSS_CLEAR_BONUS = 20
    const val PERFECT_CLEAR_BONUS = 200
    const val BOMB_PLACE_POINTS = 1
    const val BOMB_CLEAR_POINTS_PER_CELL = 2

    fun calculateMoveScore(
        placedCellCount: Int,
        clearedLineCount: Int,
        previousCombo: Int,
        isCrossClear: Boolean = false,
        isPerfectClear: Boolean = false
    ): Int {
        val placedCellPoints = placedCellCount * POINTS_PER_CELL
        val clearedLinePoints = clearedLineCount * POINTS_PER_LINE
        val multiLineBonus = calculateMultiLineBonus(clearedLineCount)
        val nextCombo = if (clearedLineCount > 0) previousCombo + 1 else 0
        val comboBonus = calculateComboBonus(nextCombo)
        val perfectClearBonus = if (isPerfectClear) PERFECT_CLEAR_BONUS else 0

        return placedCellPoints + clearedLinePoints + multiLineBonus + comboBonus + perfectClearBonus
    }

    fun calculateComboBonus(comboStreak: Int): Int {
        return when {
            comboStreak <= 1 -> 0
            comboStreak == 2 -> 10
            comboStreak == 3 -> 20
            comboStreak == 4 -> 40
            comboStreak == 5 -> 80
            else -> 160
        }
    }

    fun calculateMultiLineBonus(clearedLineCount: Int): Int {
        return if (clearedLineCount >= 2) {
            MULTI_LINE_BONUS_PER_EXTRA_LINE * (clearedLineCount - 1)
        } else {
            0
        }
    }

    fun calculateBombScore(
        clearedCellCount: Int,
        isPerfectClear: Boolean = false
    ): Int {
        val perfectClearBonus = if (isPerfectClear) PERFECT_CLEAR_BONUS else 0

        return BOMB_PLACE_POINTS + clearedCellCount * BOMB_CLEAR_POINTS_PER_CELL + perfectClearBonus
    }
}

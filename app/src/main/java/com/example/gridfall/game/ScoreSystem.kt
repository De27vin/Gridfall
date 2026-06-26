package com.example.gridfall.game

object ScoreSystem {
    const val POINTS_PER_CELL = 1
    const val POINTS_PER_LINE = 10
    const val MULTI_LINE_BONUS = 25
    const val CROSS_CLEAR_BONUS = 20
    const val BOMB_PLACE_POINTS = 1
    const val BOMB_CLEAR_POINTS_PER_CELL = 2

    fun calculateMoveScore(
        placedCellCount: Int,
        clearedLineCount: Int,
        previousCombo: Int,
        isCrossClear: Boolean = false
    ): Int {
        val placedCellPoints = placedCellCount * POINTS_PER_CELL
        val clearedLinePoints = clearedLineCount * POINTS_PER_LINE
        val multiLineBonus = if (clearedLineCount >= 2) MULTI_LINE_BONUS else 0
        val comboBonus = if (clearedLineCount > 0) previousCombo * 5 else 0
        val crossClearBonus = if (isCrossClear) CROSS_CLEAR_BONUS else 0

        return placedCellPoints + clearedLinePoints + multiLineBonus + comboBonus + crossClearBonus
    }

    fun calculateBombScore(clearedCellCount: Int): Int {
        return BOMB_PLACE_POINTS + clearedCellCount * BOMB_CLEAR_POINTS_PER_CELL
    }
}

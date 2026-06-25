package com.example.gridfall.game

object ScoreSystem {
    const val POINTS_PER_CELL = 1
    const val POINTS_PER_LINE = 10
    const val MULTI_LINE_BONUS = 25

    fun calculateMoveScore(
        placedCellCount: Int,
        clearedLineCount: Int,
        previousCombo: Int
    ): Int {
        val placedCellPoints = placedCellCount * POINTS_PER_CELL
        val clearedLinePoints = clearedLineCount * POINTS_PER_LINE
        val multiLineBonus = if (clearedLineCount >= 2) MULTI_LINE_BONUS else 0
        val comboBonus = if (clearedLineCount > 0) previousCombo * 5 else 0

        return placedCellPoints + clearedLinePoints + multiLineBonus + comboBonus
    }
}

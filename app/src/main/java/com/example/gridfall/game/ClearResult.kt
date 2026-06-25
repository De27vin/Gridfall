package com.example.gridfall.game

data class ClearResult(
    val board: Board,
    val clearedRows: List<Int>,
    val clearedColumns: List<Int>
) {
    val clearedLineCount: Int
        get() = clearedRows.size + clearedColumns.size
}

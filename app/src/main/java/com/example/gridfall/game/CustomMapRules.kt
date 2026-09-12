package com.example.gridfall.game

object CustomMapRules {
    const val BOARD_SIZE = 8
    const val MAX_ROWS = 10
    const val MAX_COLUMNS = 10
    const val MIN_PLAYABLE_CELLS = 32
    private const val MIN_PLAYABLE_PER_LINE = 3

    fun validationError(
        blockedCells: Set<Cell>,
        rows: Int = BOARD_SIZE,
        columns: Int = BOARD_SIZE
    ): String? {
        if (blockedCells.isEmpty()) {
            return "Block at least one cell to create a custom map."
        }
        if (rows !in BOARD_SIZE..MAX_ROWS || columns !in BOARD_SIZE..MAX_COLUMNS) {
            return "Custom maps can be between 8 and 10 lanes wide or tall."
        }
        if (blockedCells.any { it.row !in 0 until rows || it.col !in 0 until columns }) {
            return "The map contains cells outside the board."
        }
        val playableCount = rows * columns - blockedCells.size
        if (playableCount < MIN_PLAYABLE_CELLS) {
            return "Keep at least $MIN_PLAYABLE_CELLS playable cells."
        }
        val sparseRow = (0 until rows).any { row ->
            (0 until columns).count { col -> Cell(row, col) !in blockedCells } < MIN_PLAYABLE_PER_LINE
        }
        val sparseColumn = (0 until columns).any { col ->
            (0 until rows).count { row -> Cell(row, col) !in blockedCells } < MIN_PLAYABLE_PER_LINE
        }
        if (sparseRow || sparseColumn) {
            return "Keep at least $MIN_PLAYABLE_PER_LINE playable cells in every row and column."
        }
        return null
    }

    fun diamondBlockedCells(): Set<Cell> = buildSet {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val distance = kotlin.math.abs(row * 2 - 7) + kotlin.math.abs(col * 2 - 7)
                if (distance > 10) add(Cell(row, col))
            }
        }
    }

    fun cornerCutBlockedCells(): Set<Cell> = setOf(
        Cell(0, 0), Cell(0, 1), Cell(1, 0),
        Cell(0, 6), Cell(0, 7), Cell(1, 7),
        Cell(6, 0), Cell(7, 0), Cell(7, 1),
        Cell(6, 7), Cell(7, 6), Cell(7, 7)
    )
}

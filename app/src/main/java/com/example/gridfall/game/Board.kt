package com.example.gridfall.game

data class Board(
    val cells: List<List<Int>>,
    val blockedCells: Set<Cell> = emptySet(),
    val isCustom: Boolean = false
) {
    val size: Int
        get() = cells.size
    val rowCount: Int
        get() = cells.size
    val columnCount: Int
        get() = cells.firstOrNull()?.size ?: 0

    companion object {
        const val SIZE = 8

        fun empty(
            rows: Int = SIZE,
            columns: Int = rows,
            blockedCells: Set<Cell> = emptySet(),
            isCustom: Boolean = false
        ): Board {
            require(rows > 0 && columns > 0) { "Board dimensions must be positive" }
            require(blockedCells.all { it.row in 0 until rows && it.col in 0 until columns }) {
                "Blocked cells must be inside the board"
            }
            return Board(
                cells = List(rows) { List(columns) { 0 } },
                blockedCells = blockedCells,
                isCustom = isCustom
            )
        }

        fun centerZone(size: Int): IntRange {
            val zoneSize = if (size <= 7) 3.coerceAtMost(size) else 4.coerceAtMost(size)
            val start = (size - zoneSize) / 2
            return start until start + zoneSize
        }
    }

    fun get(row: Int, col: Int): Int {
        return cells[row][col]
    }

    fun isInside(row: Int, col: Int): Boolean {
        return row in 0 until rowCount && col in 0 until columnCount
    }

    fun isPlayable(row: Int, col: Int): Boolean {
        return isInside(row, col) && Cell(row, col) !in blockedCells
    }

    fun isEmpty(row: Int, col: Int): Boolean {
        return isPlayable(row, col) && get(row, col) == 0
    }

    fun isEmpty(): Boolean {
        return cells.all { row -> row.all { value -> value == 0 } }
    }

    fun set(row: Int, col: Int, value: Int): Board {
        val newCells = cells.mapIndexed { r, rowCells ->
            if (r == row) {
                rowCells.mapIndexed { c, oldValue ->
                    if (c == col) value else oldValue
                }
            } else {
                rowCells
            }
        }

        return copy(cells = newCells)
    }

    fun fill(row: Int, col: Int, value: Int = 1): Board {
        return set(row, col, value)
    }

}

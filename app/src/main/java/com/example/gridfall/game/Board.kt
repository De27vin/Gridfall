package com.example.gridfall.game

data class Board(
    val cells: List<List<Int>>
) {
    val size: Int
        get() = cells.size

    companion object {
        const val SIZE = 8

        fun empty(size: Int = SIZE): Board {
            require(size > 0) { "Board size must be positive" }
            return Board(
                cells = List(size) { List(size) { 0 } }
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
        return row in 0 until size && col in 0 until size
    }

    fun isEmpty(row: Int, col: Int): Boolean {
        return isInside(row, col) && get(row, col) == 0
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

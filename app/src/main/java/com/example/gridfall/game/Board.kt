package com.example.gridfall.game

data class Board(
    val cells: List<List<Int>>
) {
    companion object {
        const val SIZE = 8

        fun empty(): Board {
            return Board(
                cells = List(SIZE) { List(SIZE) { 0 } }
            )
        }
    }

    fun get(row: Int, col: Int): Int {
        return cells[row][col]
    }

    fun isInside(row: Int, col: Int): Boolean {
        return row in 0 until SIZE && col in 0 until SIZE
    }

    fun isEmpty(row: Int, col: Int): Boolean {
        return isInside(row, col) && get(row, col) == 0
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

    fun fill(row: Int, col: Int): Board {
        return set(row, col, 1)
    }
}

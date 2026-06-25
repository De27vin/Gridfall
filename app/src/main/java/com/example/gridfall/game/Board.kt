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
}

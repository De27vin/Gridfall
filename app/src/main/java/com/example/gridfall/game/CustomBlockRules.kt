package com.example.gridfall.game

object CustomBlockRules {
    const val EDITOR_SIZE = 4

    fun validationError(cells: Set<Cell>): String? {
        if (cells.isEmpty()) return "Select at least one cell for this block."
        if (cells.any { it.row !in 0 until EDITOR_SIZE || it.col !in 0 until EDITOR_SIZE }) {
            return "The custom block must stay inside the 4×4 editor."
        }

        return null
    }

    fun validationError(
        cells: Set<Cell>,
        boardRows: Int,
        boardColumns: Int
    ): String? {
        validationError(cells)?.let { return it }

        val normalized = normalize(cells)
        val blockRows = normalized.maxOf(Cell::row) + 1
        val blockColumns = normalized.maxOf(Cell::col) + 1
        return if (blockRows <= boardRows && blockColumns <= boardColumns) {
            null
        } else {
            "The custom block must fit inside this map."
        }
    }

    fun normalize(cells: Set<Cell>): Set<Cell> {
        if (cells.isEmpty()) return emptySet()
        val top = cells.minOf(Cell::row)
        val left = cells.minOf(Cell::col)
        return cells.mapTo(mutableSetOf()) { Cell(it.row - top, it.col - left) }
    }

    fun toPiece(cells: Set<Cell>): Piece? {
        if (cells.isEmpty() || validationError(cells) != null) return null
        return Piece(
            id = "custom_map_block",
            cells = normalize(cells).sortedWith(compareBy(Cell::row, Cell::col)),
            rarity = PieceRarity.Common,
            colorVariant = 2
        )
    }
}

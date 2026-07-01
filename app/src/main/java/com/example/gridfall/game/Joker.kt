package com.example.gridfall.game

enum class JokerType(val title: String) {
    Single("1x1 Joker"),
    HorizontalTwo("2x1 Joker"),
    VerticalTwo("1x2 Joker"),
    Bomb("Bomb Joker"),
    Revert("Revert Joker");

    fun toPiece(): Piece? {
        return when (this) {
            Single -> Piece(
                id = "joker_single",
                cells = listOf(Cell(0, 0)),
                colorVariant = 4
            )
            HorizontalTwo -> Piece(
                id = "joker_horizontal_2",
                cells = listOf(Cell(0, 0), Cell(0, 1)),
                colorVariant = 3
            )
            VerticalTwo -> Piece(
                id = "joker_vertical_2",
                cells = listOf(Cell(0, 0), Cell(1, 0)),
                colorVariant = 2
            )
            Bomb -> PieceLibrary.bombPiece.copy(id = "joker_bomb")
            Revert -> null
        }
    }
}

data class RiskSpinState(
    val inventory: List<JokerType> = emptyList(),
    val cooldownBatchesRemaining: Int = 0,
    val previousMoveSnapshot: GameState? = null
) {
    val isInventoryFull: Boolean
        get() = inventory.size >= MAX_INVENTORY_SIZE

    companion object {
        const val MAX_INVENTORY_SIZE = 5
    }
}

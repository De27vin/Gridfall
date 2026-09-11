package com.example.gridfall.game

enum class GridLayoutPreset(
    val id: String,
    val title: String,
    val boardSize: Int,
    val piecesPerBatch: Int,
    val description: String
) {
    Rush(
        id = "rush_7",
        title = "Rush",
        boardSize = 7,
        piecesPerBatch = 3,
        description = "Tight and fast"
    ),
    Classic(
        id = "classic_8",
        title = "Classic",
        boardSize = 8,
        piecesPerBatch = 3,
        description = "The balanced original"
    ),
    Marathon(
        id = "marathon_10",
        title = "Marathon",
        boardSize = 10,
        piecesPerBatch = 4,
        description = "More room to build"
    );

    val sizeLabel: String
        get() = "${boardSize}×${boardSize}"

    companion object {
        fun fromId(id: String?): GridLayoutPreset {
            return entries.firstOrNull { it.id == id } ?: Classic
        }

        fun fromBoardSize(boardSize: Int): GridLayoutPreset {
            return entries.firstOrNull { it.boardSize == boardSize } ?: Classic
        }
    }
}

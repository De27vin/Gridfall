package com.example.gridfall.game

object PieceLibrary {
    val bombPiece = Piece(
        id = "bomb_single",
        cells = listOf(Cell(0, 0)),
        effect = PieceEffect.Bomb
    )

    val starterPieces: List<Piece> = listOf(
        Piece(
            id = "single",
            cells = listOf(Cell(0, 0))
        ),
        Piece(
            id = "horizontal_2",
            cells = listOf(Cell(0, 0), Cell(0, 1))
        ),
        Piece(
            id = "vertical_2",
            cells = listOf(Cell(0, 0), Cell(1, 0))
        ),
        Piece(
            id = "horizontal_3",
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2))
        ),
        Piece(
            id = "vertical_3",
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0))
        ),
        Piece(
            id = "horizontal_4",
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3))
        ),
        Piece(
            id = "vertical_4",
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0))
        ),
        Piece(
            id = "square_2x2",
            cells = listOf(
                Cell(0, 0), Cell(0, 1),
                Cell(1, 0), Cell(1, 1)
            )
        ),
        Piece(
            id = "small_l",
            cells = listOf(
                Cell(0, 0),
                Cell(1, 0),
                Cell(1, 1)
            )
        ),
        Piece(
            id = "small_l_mirrored",
            cells = listOf(
                Cell(0, 1),
                Cell(1, 0),
                Cell(1, 1)
            )
        ),
        Piece(
            id = "corner_3",
            cells = listOf(
                Cell(0, 0),
                Cell(0, 1),
                Cell(1, 0)
            )
        )
    )
}

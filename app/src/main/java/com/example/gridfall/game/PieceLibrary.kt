package com.example.gridfall.game

object PieceLibrary {
    val bombPiece = Piece(
        id = "bomb_single",
        cells = listOf(Cell(0, 0)),
        effect = PieceEffect.Bomb,
        rarity = PieceRarity.Rare
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
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3)),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "vertical_4",
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0)),
            rarity = PieceRarity.Uncommon
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
        ),
        Piece(
            id = "t_4",
            cells = listOf(
                Cell(0, 0), Cell(0, 1), Cell(0, 2),
                Cell(1, 1)
            ),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "t_4_down",
            cells = listOf(
                Cell(0, 1),
                Cell(1, 0), Cell(1, 1), Cell(1, 2)
            ),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "l_4",
            cells = listOf(
                Cell(0, 0),
                Cell(1, 0),
                Cell(2, 0), Cell(2, 1)
            ),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "l_4_mirrored",
            cells = listOf(
                Cell(0, 1),
                Cell(1, 1),
                Cell(2, 0), Cell(2, 1)
            ),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "s_4",
            cells = listOf(
                Cell(0, 1), Cell(0, 2),
                Cell(1, 0), Cell(1, 1)
            ),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "z_4",
            cells = listOf(
                Cell(0, 0), Cell(0, 1),
                Cell(1, 1), Cell(1, 2)
            ),
            rarity = PieceRarity.Uncommon
        ),
        Piece(
            id = "horizontal_5",
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3), Cell(0, 4)),
            rarity = PieceRarity.Rare
        ),
        Piece(
            id = "vertical_5",
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0), Cell(4, 0)),
            rarity = PieceRarity.Rare
        ),
        Piece(
            id = "plus_5",
            cells = listOf(
                Cell(0, 1),
                Cell(1, 0), Cell(1, 1), Cell(1, 2),
                Cell(2, 1)
            ),
            rarity = PieceRarity.Rare
        ),
        Piece(
            id = "big_l_5",
            cells = listOf(
                Cell(0, 0),
                Cell(1, 0),
                Cell(2, 0),
                Cell(3, 0), Cell(3, 1)
            ),
            rarity = PieceRarity.Rare
        ),
        Piece(
            id = "big_l_5_mirrored",
            cells = listOf(
                Cell(0, 1),
                Cell(1, 1),
                Cell(2, 1),
                Cell(3, 0), Cell(3, 1)
            ),
            rarity = PieceRarity.Rare
        ),
        Piece(
            id = "square_3x3",
            cells = listOf(
                Cell(0, 0), Cell(0, 1), Cell(0, 2),
                Cell(1, 0), Cell(1, 1), Cell(1, 2),
                Cell(2, 0), Cell(2, 1), Cell(2, 2)
            ),
            rarity = PieceRarity.Epic
        )
    )
}

package com.example.gridfall.game

object GameEngine {
    fun createInitialState(): GameState {
        return GameState(
            board = Board.empty(),
            currentPieces = PieceGenerator.generateBatch(),
            usedPieceIndices = emptySet(),
            score = 0,
            combo = 0,
            isGameOver = false
        )
    }

    fun canPlace(
        board: Board,
        piece: Piece,
        startRow: Int,
        startCol: Int
    ): Boolean {
        return piece.cells.all { cell ->
            val row = startRow + cell.row
            val col = startCol + cell.col

            board.isEmpty(row, col)
        }
    }

    fun findFullRows(board: Board): List<Int> {
        return (0 until Board.SIZE).filter { row ->
            (0 until Board.SIZE).all { col ->
                board.get(row, col) == 1
            }
        }
    }

    fun findFullColumns(board: Board): List<Int> {
        return (0 until Board.SIZE).filter { col ->
            (0 until Board.SIZE).all { row ->
                board.get(row, col) == 1
            }
        }
    }

    fun clearLines(board: Board): ClearResult {
        val clearedRows = findFullRows(board)
        val clearedColumns = findFullColumns(board)
        val clearedRowSet = clearedRows.toSet()
        val clearedColumnSet = clearedColumns.toSet()

        var clearedBoard = board
        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                if (row in clearedRowSet || col in clearedColumnSet) {
                    clearedBoard = clearedBoard.set(row, col, 0)
                }
            }
        }

        return ClearResult(
            board = clearedBoard,
            clearedRows = clearedRows,
            clearedColumns = clearedColumns
        )
    }

    fun getAvailablePieces(state: GameState): List<Pair<Int, Piece>> {
        return state.currentPieces.mapIndexedNotNull { index, piece ->
            if (index in state.usedPieceIndices) {
                null
            } else {
                index to piece
            }
        }
    }

    fun hasAnyValidMove(
        board: Board,
        pieces: List<Piece>
    ): Boolean {
        return pieces.any { piece ->
            (0 until Board.SIZE).any { row ->
                (0 until Board.SIZE).any { col ->
                    canPlace(board, piece, row, col)
                }
            }
        }
    }

    fun placePiece(
        state: GameState,
        pieceIndex: Int,
        startRow: Int,
        startCol: Int
    ): GameState {
        if (state.isGameOver) return state
        if (pieceIndex !in state.currentPieces.indices) return state
        if (pieceIndex in state.usedPieceIndices) return state

        val piece = state.currentPieces[pieceIndex]
        if (!canPlace(state.board, piece, startRow, startCol)) return state

        val placedBoard = placeCellsOnBoard(state.board, piece, startRow, startCol)
        val clearResult = clearLines(placedBoard)
        val scoreGained = ScoreSystem.calculateMoveScore(
            placedCellCount = piece.cells.size,
            clearedLineCount = clearResult.clearedLineCount,
            previousCombo = state.combo
        )
        val nextCombo = if (clearResult.clearedLineCount > 0) state.combo + 1 else 0
        val nextUsedPieceIndices = state.usedPieceIndices + pieceIndex
        val allPiecesUsed = state.currentPieces.indices.all { it in nextUsedPieceIndices }

        val nextPieces: List<Piece>
        val finalUsedPieceIndices: Set<Int>
        if (allPiecesUsed) {
            nextPieces = PieceGenerator.generateBatch()
            finalUsedPieceIndices = emptySet()
        } else {
            nextPieces = state.currentPieces
            finalUsedPieceIndices = nextUsedPieceIndices
        }

        val availablePieces = nextPieces.filterIndexed { index, _ ->
            index !in finalUsedPieceIndices
        }
        val isGameOver = !hasAnyValidMove(clearResult.board, availablePieces)

        return state.copy(
            board = clearResult.board,
            currentPieces = nextPieces,
            usedPieceIndices = finalUsedPieceIndices,
            score = state.score + scoreGained,
            combo = nextCombo,
            isGameOver = isGameOver
        )
    }

    private fun placeCellsOnBoard(
        board: Board,
        piece: Piece,
        startRow: Int,
        startCol: Int
    ): Board {
        var updatedBoard = board
        piece.cells.forEach { cell ->
            updatedBoard = updatedBoard.fill(
                row = startRow + cell.row,
                col = startCol + cell.col
            )
        }

        return updatedBoard
    }
}

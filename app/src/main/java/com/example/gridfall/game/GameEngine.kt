package com.example.gridfall.game

object GameEngine {
    fun createInitialState(): GameState {
        return GameState(
            board = Board.empty(),
            currentPieces = PieceGenerator.generateBatch(),
            usedPieceIndices = emptySet(),
            score = 0,
            combo = 0,
            isGameOver = false,
            contractState = ContractState(
                offeredContract = ContractGenerator.generate()
            )
        )
    }

    fun acceptContract(state: GameState): GameState {
        if (state.isGameOver) return state
        if (state.usedPieceIndices.isNotEmpty()) return state
        val contract = state.contractState.offeredContract ?: return state

        return state.copy(
            contractState = ContractState(
                activeContract = contract,
                isAccepted = true
            )
        )
    }

    fun skipContract(state: GameState): GameState {
        if (state.isGameOver) return state
        if (state.contractState.offeredContract == null) return state

        return state.copy(
            contractState = state.contractState.copy(
                offeredContract = null,
                activeContract = null,
                resolvedContract = null,
                isAccepted = false,
                isCompleted = false,
                isFailed = false,
                rewardClaimed = false
            )
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
        val updatedContractState = updateContractProgress(
            contractState = state.contractState,
            piece = piece,
            startRow = startRow,
            startCol = startCol,
            clearedLineCount = clearResult.clearedLineCount,
            scoreGained = scoreGained
        )
        val nextCombo = if (clearResult.clearedLineCount > 0) state.combo + 1 else 0
        val nextUsedPieceIndices = state.usedPieceIndices + pieceIndex
        val allPiecesUsed = state.currentPieces.indices.all { it in nextUsedPieceIndices }

        val nextPieces: List<Piece>
        val finalUsedPieceIndices: Set<Int>
        val finalContractState: ContractState
        val rewardPoints: Int
        if (allPiecesUsed) {
            nextPieces = PieceGenerator.generateBatch()
            finalUsedPieceIndices = emptySet()
            val evaluation = finishContractBatch(updatedContractState)
            finalContractState = evaluation.contractState
            rewardPoints = evaluation.rewardPoints
        } else {
            nextPieces = state.currentPieces
            finalUsedPieceIndices = nextUsedPieceIndices
            finalContractState = updatedContractState
            rewardPoints = 0
        }

        val availablePieces = nextPieces.filterIndexed { index, _ ->
            index !in finalUsedPieceIndices
        }
        val isGameOver = !hasAnyValidMove(clearResult.board, availablePieces)

        return state.copy(
            board = clearResult.board,
            currentPieces = nextPieces,
            usedPieceIndices = finalUsedPieceIndices,
            score = state.score + scoreGained + rewardPoints,
            combo = nextCombo,
            isGameOver = isGameOver,
            contractState = finalContractState
        )
    }

    private fun updateContractProgress(
        contractState: ContractState,
        piece: Piece,
        startRow: Int,
        startCol: Int,
        clearedLineCount: Int,
        scoreGained: Int
    ): ContractState {
        if (!contractState.isAccepted || contractState.activeContract == null) {
            return contractState.copy(
                offeredContract = null,
                resolvedContract = null,
                isCompleted = false,
                isFailed = false,
                rewardClaimed = false
            )
        }

        val placedCells = absoluteCells(piece, startRow, startCol)

        return contractState.copy(
            batchPlacedPieces = contractState.batchPlacedPieces + 1,
            batchClearedLines = contractState.batchClearedLines + clearedLineCount,
            batchScoreGained = contractState.batchScoreGained + scoreGained,
            usedEdge = contractState.usedEdge || placedCells.any { cell ->
                cell.row == 0 ||
                    cell.row == Board.SIZE - 1 ||
                    cell.col == 0 ||
                    cell.col == Board.SIZE - 1
            },
            usedCenter = contractState.usedCenter || placedCells.any { cell ->
                cell.row in 2..5 && cell.col in 2..5
            },
            resolvedContract = null,
            isCompleted = false,
            isFailed = false,
            rewardClaimed = false
        )
    }

    private fun finishContractBatch(contractState: ContractState): ContractEvaluation {
        val activeContract = contractState.activeContract
        if (!contractState.isAccepted || activeContract == null) {
            return ContractEvaluation(
                contractState = ContractState(
                    offeredContract = ContractGenerator.generate()
                ),
                rewardPoints = 0
            )
        }

        val completed = when (activeContract.type) {
            ContractType.ClearAtLeastOneLine -> contractState.batchClearedLines >= 1
            ContractType.ClearExactlyTwoLines -> contractState.batchClearedLines == 2
            ContractType.NoEdgePlacement -> !contractState.usedEdge
            ContractType.AvoidCenterArea -> !contractState.usedCenter
            ContractType.ScoreAtLeastTwenty -> contractState.batchScoreGained >= 20
        }
        val rewardPoints = if (completed) activeContract.rewardPoints else 0

        return ContractEvaluation(
            contractState = ContractState(
                offeredContract = ContractGenerator.generate(),
                resolvedContract = activeContract,
                isCompleted = completed,
                isFailed = !completed,
                rewardClaimed = completed
            ),
            rewardPoints = rewardPoints
        )
    }

    private fun absoluteCells(
        piece: Piece,
        startRow: Int,
        startCol: Int
    ): List<Cell> {
        return piece.cells.map { cell ->
            Cell(
                row = startRow + cell.row,
                col = startCol + cell.col
            )
        }
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

    private data class ContractEvaluation(
        val contractState: ContractState,
        val rewardPoints: Int
    )
}

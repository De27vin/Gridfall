package com.example.gridfall.game

object GameEngine {
    fun createInitialState(): GameState {
        val level = LevelSystem.levelForScore(0)

        return GameState(
            board = Board.empty(),
            currentPieces = PieceGenerator.generateBatch(level = level),
            usedPieceIndices = emptySet(),
            score = 0,
            combo = 0,
            isGameOver = false,
            contractState = ContractState(
                batchesUntilNextOffer = ContractGenerator.initialOfferCooldown()
            )
        )
    }

    fun acceptContract(state: GameState): GameState {
        if (state.isGameOver) return state
        if (state.usedPieceIndices.isNotEmpty()) return state
        val contract = state.contractState.offeredContract ?: return state

        return state.copy(
            contractState = state.contractState.copy(
                offeredContract = null,
                activeContract = contract,
                resolvedContract = null,
                isAccepted = true,
                isCompleted = false,
                isFailed = false,
                rewardClaimed = false,
                penaltyApplied = false
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
                rewardClaimed = false,
                penaltyApplied = false,
                batchesUntilNextOffer = ContractGenerator.nextOfferCooldown()
            )
        )
    }

    fun canPlace(
        board: Board,
        piece: Piece,
        startRow: Int,
        startCol: Int
    ): Boolean {
        if (piece.effect == PieceEffect.Bomb) {
            val cell = piece.cells.singleOrNull() ?: return false
            return board.isEmpty(
                row = startRow + cell.row,
                col = startCol + cell.col
            )
        }

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

        val placementResult = placePieceOnBoard(
            state = state,
            piece = piece,
            startRow = startRow,
            startCol = startCol
        )
        val updatedContractState = updateContractProgress(
            contractState = state.contractState,
            piece = piece,
            startRow = startRow,
            startCol = startCol,
            clearedLineCount = placementResult.clearedLineCount,
            scoreGained = placementResult.scoreGained
        )
        val nextUsedPieceIndices = state.usedPieceIndices + pieceIndex
        val allPiecesUsed = state.currentPieces.indices.all { it in nextUsedPieceIndices }
        val scoreAfterPlacement = state.score + placementResult.scoreGained

        val nextPieces: List<Piece>
        val finalUsedPieceIndices: Set<Int>
        val finalContractState: ContractState
        val rewardPoints: Int
        if (allPiecesUsed) {
            finalUsedPieceIndices = emptySet()
            val evaluation = evaluateContractBatch(updatedContractState)
            rewardPoints = evaluation.scoreDelta
            val finalScore = (scoreAfterPlacement + rewardPoints).coerceAtLeast(0)
            val nextLevel = LevelSystem.levelForScore(finalScore)
            nextPieces = PieceGenerator.generateBatch(level = nextLevel)
            finalContractState = advanceContractBatch(
                contractState = evaluation.contractState,
                level = nextLevel
            )
        } else {
            nextPieces = state.currentPieces
            finalUsedPieceIndices = nextUsedPieceIndices
            finalContractState = updatedContractState
            rewardPoints = 0
        }

        val availablePieces = nextPieces.filterIndexed { index, _ ->
            index !in finalUsedPieceIndices
        }
        val isGameOver = !hasAnyValidMove(placementResult.board, availablePieces)

        return state.copy(
            board = placementResult.board,
            currentPieces = nextPieces,
            usedPieceIndices = finalUsedPieceIndices,
            score = (scoreAfterPlacement + rewardPoints).coerceAtLeast(0),
            combo = placementResult.nextCombo,
            isGameOver = isGameOver,
            contractState = finalContractState
        )
    }

    private fun placePieceOnBoard(
        state: GameState,
        piece: Piece,
        startRow: Int,
        startCol: Int
    ): PlacementResult {
        if (piece.effect == PieceEffect.Bomb) {
            val bombResult = explodeBomb(
                board = state.board,
                piece = piece,
                startRow = startRow,
                startCol = startCol
            )

            return PlacementResult(
                board = bombResult.board,
                clearedLineCount = 0,
                scoreGained = ScoreSystem.calculateBombScore(bombResult.clearedCellCount),
                nextCombo = 0
            )
        }

        val placedBoard = placeCellsOnBoard(state.board, piece, startRow, startCol)
        val clearResult = clearLines(placedBoard)
        val scoreGained = ScoreSystem.calculateMoveScore(
            placedCellCount = piece.cells.size,
            clearedLineCount = clearResult.clearedLineCount,
            previousCombo = state.combo
        )

        return PlacementResult(
            board = clearResult.board,
            clearedLineCount = clearResult.clearedLineCount,
            scoreGained = scoreGained,
            nextCombo = if (clearResult.clearedLineCount > 0) state.combo + 1 else 0
        )
    }

    private fun explodeBomb(
        board: Board,
        piece: Piece,
        startRow: Int,
        startCol: Int
    ): BombResult {
        val bombCell = piece.cells.single()
        val centerRow = startRow + bombCell.row
        val centerCol = startCol + bombCell.col
        var updatedBoard = board
        var clearedCellCount = 0

        for (row in centerRow - 1..centerRow + 1) {
            for (col in centerCol - 1..centerCol + 1) {
                if (board.isInside(row, col) && board.get(row, col) == 1) {
                    updatedBoard = updatedBoard.set(row, col, 0)
                    clearedCellCount += 1
                }
            }
        }

        updatedBoard = updatedBoard.set(centerRow, centerCol, 0)

        return BombResult(
            board = updatedBoard,
            clearedCellCount = clearedCellCount
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
            rewardClaimed = false,
            penaltyApplied = false
        )
    }

    private fun evaluateContractBatch(contractState: ContractState): ContractEvaluation {
        val activeContract = contractState.activeContract
        if (!contractState.isAccepted || activeContract == null) {
            return ContractEvaluation(
                contractState = contractState.copy(
                    offeredContract = null,
                    activeContract = null,
                    resolvedContract = null,
                    isAccepted = false,
                    isCompleted = false,
                    isFailed = false,
                    batchPlacedPieces = 0,
                    batchClearedLines = 0,
                    batchScoreGained = 0,
                    usedEdge = false,
                    usedCenter = false,
                    rewardClaimed = false,
                    penaltyApplied = false
                ),
                scoreDelta = 0
            )
        }

        val completed = when (activeContract.type) {
            ContractType.ClearAtLeastOneLine -> contractState.batchClearedLines >= 1
            ContractType.ClearExactlyTwoLines -> contractState.batchClearedLines == 2
            ContractType.NoEdgePlacement -> !contractState.usedEdge
            ContractType.AvoidCenterArea -> !contractState.usedCenter
            ContractType.ScoreAtLeastTwenty -> contractState.batchScoreGained >= 20
        }
        val scoreDelta = if (completed) activeContract.rewardPoints else -activeContract.penaltyPoints

        return ContractEvaluation(
            contractState = ContractState(
                resolvedContract = activeContract,
                isCompleted = completed,
                isFailed = !completed,
                rewardClaimed = completed,
                penaltyApplied = !completed,
                completedBatchCount = contractState.completedBatchCount
            ),
            scoreDelta = scoreDelta
        )
    }

    private fun advanceContractBatch(
        contractState: ContractState,
        level: Int
    ): ContractState {
        val completedBatchCount = contractState.completedBatchCount + 1
        val resolvedContract = contractState.resolvedContract

        if (resolvedContract != null) {
            return contractState.copy(
                completedBatchCount = completedBatchCount,
                batchesUntilNextOffer = ContractGenerator.nextOfferCooldown()
            )
        }

        if (contractState.offeredContract != null || contractState.activeContract != null) {
            return contractState.copy(completedBatchCount = completedBatchCount)
        }

        val batchesUntilNextOffer = (contractState.batchesUntilNextOffer - 1).coerceAtLeast(0)
        return if (batchesUntilNextOffer == 0) {
            contractState.copy(
                offeredContract = ContractGenerator.generate(level = level),
                batchesUntilNextOffer = 0,
                completedBatchCount = completedBatchCount
            )
        } else {
            contractState.copy(
                batchesUntilNextOffer = batchesUntilNextOffer,
                completedBatchCount = completedBatchCount
            )
        }
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
        val scoreDelta: Int
    )

    private data class PlacementResult(
        val board: Board,
        val clearedLineCount: Int,
        val scoreGained: Int,
        val nextCombo: Int
    )
}

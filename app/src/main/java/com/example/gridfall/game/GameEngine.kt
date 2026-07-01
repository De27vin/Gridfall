package com.example.gridfall.game

import kotlin.random.Random

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
                board.get(row, col) != 0
            }
        }
    }

    fun findFullColumns(board: Board): List<Int> {
        return (0 until Board.SIZE).filter { col ->
            (0 until Board.SIZE).all { row ->
                board.get(row, col) != 0
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

    fun riskSpinAvailability(state: GameState): RiskSpinAvailability {
        val level = LevelSystem.levelForScore(state.score)
        return when {
            state.isGameOver -> RiskSpinAvailability(false, "Game over")
            level < 3 -> RiskSpinAvailability(false, "Unlocks at Level 3")
            state.riskSpinState.cooldownBatchesRemaining > 0 -> RiskSpinAvailability(
                false,
                "Available in ${state.riskSpinState.cooldownBatchesRemaining} batches"
            )
            state.riskSpinState.isInventoryFull -> RiskSpinAvailability(false, "Inventory full")
            RiskSpinOption.entries.none { canSelectRiskSpinOption(state, it) } -> RiskSpinAvailability(
                false,
                "Need enough score to stay Level 3"
            )
            else -> RiskSpinAvailability(true)
        }
    }

    fun riskSpinCost(state: GameState, option: RiskSpinOption): Int {
        return option.cost(state.score)
    }

    fun canSelectRiskSpinOption(state: GameState, option: RiskSpinOption): Boolean {
        val scoreAfterPayment = state.score - riskSpinCost(state, option)
        return scoreAfterPayment >= 0 &&
            LevelSystem.levelForScore(scoreAfterPayment) >= 3
    }

    fun performRiskSpin(
        state: GameState,
        option: RiskSpinOption,
        random: Random = Random.Default
    ): RiskSpinResult? {
        if (!riskSpinAvailability(state).isAvailable) return null
        if (!canSelectRiskSpinOption(state, option)) return null

        val cost = riskSpinCost(state, option)
        val (nextInventory, entries) = RiskSpin.spinEntries(
            spinCount = option.spinCount,
            startingInventory = state.riskSpinState.inventory,
            random = random
        )
        val nextState = state.copy(
            score = (state.score - cost).coerceAtLeast(0),
            riskSpinState = state.riskSpinState.copy(
                inventory = nextInventory.take(RiskSpinState.MAX_INVENTORY_SIZE),
                cooldownBatchesRemaining = RiskSpin.randomCooldown(random)
            )
        )

        return RiskSpinResult(
            state = nextState,
            option = option,
            cost = cost,
            entries = entries
        )
    }

    fun startRiskSpinMemorySession(
        state: GameState,
        option: RiskSpinOption,
        random: Random = Random.Default
    ): RiskSpinMemoryStartResult? {
        if (!riskSpinAvailability(state).isAvailable) return null
        if (!canSelectRiskSpinOption(state, option)) return null

        val cost = riskSpinCost(state, option)
        val nextState = state.copy(
            score = (state.score - cost).coerceAtLeast(0),
            riskSpinState = state.riskSpinState.copy(
                cooldownBatchesRemaining = RiskSpin.randomCooldown(random)
            )
        )

        return RiskSpinMemoryStartResult(
            state = nextState,
            session = RiskSpin.createMemorySession(
                option = option,
                random = random
            ),
            cost = cost
        )
    }

    fun revealRiskSpinMemoryField(
        state: GameState,
        session: RiskSpinMemorySession,
        fieldId: Int
    ): RiskSpinMemoryRevealResult? {
        if (session.revealsLeft <= 0) return null
        val field = session.fields.firstOrNull { it.id == fieldId } ?: return null
        if (field.isRevealed) return null

        val joker = field.outcome.toJokerType()
        val canAddJoker = joker != null &&
            state.riskSpinState.inventory.size < RiskSpinState.MAX_INVENTORY_SIZE
        val nextInventory = if (canAddJoker && joker != null) {
            state.riskSpinState.inventory + joker
        } else {
            state.riskSpinState.inventory
        }
        val revealedField = field.copy(
            isRevealed = true,
            jokerAdded = if (canAddJoker) joker else null,
            lostBecauseInventoryFull = joker != null && !canAddJoker
        )
        val nextFields = session.fields.map { existingField ->
            if (existingField.id == fieldId) revealedField else existingField
        }
        val entry = RiskSpinResultEntry(
            outcome = field.outcome,
            jokerAdded = revealedField.jokerAdded,
            lostBecauseInventoryFull = revealedField.lostBecauseInventoryFull
        )

        return RiskSpinMemoryRevealResult(
            state = state.copy(
                riskSpinState = state.riskSpinState.copy(inventory = nextInventory)
            ),
            session = session.copy(
                revealsLeft = (session.revealsLeft - 1).coerceAtLeast(0),
                fields = nextFields
            ),
            entry = entry
        )
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

        val previousSnapshot = snapshotBeforeMove(state)
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
        val immediateContractEvaluation = evaluateImmediateContractFailure(updatedContractState)
        val nextUsedPieceIndices = state.usedPieceIndices + pieceIndex
        val allPiecesUsed = state.currentPieces.indices.all { it in nextUsedPieceIndices }
        val scoreAfterPlacement = (
            state.score +
                placementResult.scoreGained +
                immediateContractEvaluation.scoreDelta
            ).coerceAtLeast(0)

        val nextPieces: List<Piece>
        val finalUsedPieceIndices: Set<Int>
        val finalContractState: ContractState
        val advancedRiskSpinState: RiskSpinState
        val contractScoreDelta: Int
        if (allPiecesUsed) {
            finalUsedPieceIndices = emptySet()
            val evaluation = evaluateContractBatch(immediateContractEvaluation.contractState)
            contractScoreDelta = evaluation.scoreDelta
            val finalScore = (scoreAfterPlacement + contractScoreDelta).coerceAtLeast(0)
            val nextLevel = LevelSystem.levelForScore(finalScore)
            nextPieces = PieceGenerator.generateBatch(level = nextLevel)
            finalContractState = advanceContractBatch(
                contractState = evaluation.contractState,
                level = nextLevel
            )
            advancedRiskSpinState = advanceRiskSpinBatch(state.riskSpinState)
        } else {
            nextPieces = state.currentPieces
            finalUsedPieceIndices = nextUsedPieceIndices
            finalContractState = immediateContractEvaluation.contractState
            contractScoreDelta = 0
            advancedRiskSpinState = state.riskSpinState
        }

        val availablePieces = nextPieces.filterIndexed { index, _ ->
            index !in finalUsedPieceIndices
        }
        val finalRiskSpinState = advancedRiskSpinState.copy(previousMoveSnapshot = previousSnapshot)
        val isGameOver = !hasAnyValidMove(
            placementResult.board,
            availablePieces + placeableJokerPieces(finalRiskSpinState.inventory)
        )

        return state.copy(
            board = placementResult.board,
            currentPieces = nextPieces,
            usedPieceIndices = finalUsedPieceIndices,
            score = (scoreAfterPlacement + contractScoreDelta).coerceAtLeast(0),
            combo = placementResult.nextCombo,
            isGameOver = isGameOver,
            contractState = finalContractState,
            riskSpinState = finalRiskSpinState
        )
    }

    fun placeJoker(
        state: GameState,
        jokerType: JokerType,
        startRow: Int,
        startCol: Int
    ): GameState {
        if (state.isGameOver) return state
        if (jokerType !in state.riskSpinState.inventory) return state
        val piece = jokerType.toPiece() ?: return state
        if (!canPlace(state.board, piece, startRow, startCol)) return state

        val previousSnapshot = snapshotBeforeMove(state)
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
        val immediateContractEvaluation = evaluateImmediateContractFailure(updatedContractState)
        val scoreAfterPlacement = (
            state.score +
                placementResult.scoreGained +
                immediateContractEvaluation.scoreDelta
            ).coerceAtLeast(0)
        val nextInventory = removeOneJoker(state.riskSpinState.inventory, jokerType)
        val nextRiskSpinState = state.riskSpinState.copy(
            inventory = nextInventory,
            previousMoveSnapshot = previousSnapshot
        )
        val availablePieces = state.currentPieces.filterIndexed { index, _ ->
            index !in state.usedPieceIndices
        }
        val isGameOver = !hasAnyValidMove(
            placementResult.board,
            availablePieces + placeableJokerPieces(nextInventory)
        )

        return state.copy(
            board = placementResult.board,
            score = scoreAfterPlacement,
            combo = placementResult.nextCombo,
            isGameOver = isGameOver,
            contractState = immediateContractEvaluation.contractState,
            riskSpinState = nextRiskSpinState
        )
    }

    fun useRevertJoker(state: GameState): GameState {
        if (JokerType.Revert !in state.riskSpinState.inventory) return state
        val snapshot = state.riskSpinState.previousMoveSnapshot ?: return state
        val restoredInventory = removeOneJoker(
            inventory = snapshot.riskSpinState.inventory,
            jokerType = JokerType.Revert
        )

        return snapshot.copy(
            riskSpinState = snapshot.riskSpinState.copy(
                inventory = restoredInventory,
                previousMoveSnapshot = null
            ),
            isGameOver = false
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
                scoreGained = ScoreSystem.calculateBombScore(
                    clearedCellCount = bombResult.clearedCellCount,
                    isPerfectClear = bombResult.clearedCellCount > 0 && bombResult.board.isEmpty()
                ),
                nextCombo = 0
            )
        }

        val placedBoard = placeCellsOnBoard(state.board, piece, startRow, startCol)
        val clearResult = clearLines(placedBoard)
        val isCrossClear = clearResult.clearedRows.isNotEmpty() && clearResult.clearedColumns.isNotEmpty()
        val isPerfectClear = clearResult.board.isEmpty()
        val scoreGained = ScoreSystem.calculateMoveScore(
            placedCellCount = piece.cells.size,
            clearedLineCount = clearResult.clearedLineCount,
            previousCombo = state.combo,
            isCrossClear = isCrossClear,
            isPerfectClear = isPerfectClear
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
                if (board.isInside(row, col) && board.get(row, col) != 0) {
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

    private fun evaluateImmediateContractFailure(contractState: ContractState): ContractEvaluation {
        val activeContract = contractState.activeContract
        if (!contractState.isAccepted || activeContract == null) {
            return ContractEvaluation(contractState = contractState, scoreDelta = 0)
        }

        val failed = when (activeContract.type) {
            ContractType.NoEdgePlacement -> contractState.usedEdge
            ContractType.AvoidCenterArea -> contractState.usedCenter
            ContractType.ClearAtLeastOneLine,
            ContractType.ClearExactlyTwoLines,
            ContractType.ScoreAtLeastTwenty -> false
        }

        if (!failed) {
            return ContractEvaluation(contractState = contractState, scoreDelta = 0)
        }

        return ContractEvaluation(
            contractState = ContractState(
                resolvedContract = activeContract,
                isFailed = true,
                penaltyApplied = true,
                batchesUntilNextOffer = ContractGenerator.nextOfferCooldown(),
                completedBatchCount = contractState.completedBatchCount
            ),
            scoreDelta = -activeContract.penaltyPoints
        )
    }

    private fun evaluateContractBatch(contractState: ContractState): ContractEvaluation {
        if (contractState.resolvedContract != null) {
            return ContractEvaluation(
                contractState = contractState,
                scoreDelta = 0
            )
        }

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
                col = startCol + cell.col,
                value = piece.colorVariant
            )
        }

        return updatedBoard
    }

    private fun snapshotBeforeMove(state: GameState): GameState {
        return state.copy(
            riskSpinState = state.riskSpinState.copy(previousMoveSnapshot = null)
        )
    }

    private fun advanceRiskSpinBatch(riskSpinState: RiskSpinState): RiskSpinState {
        return riskSpinState.copy(
            cooldownBatchesRemaining = (riskSpinState.cooldownBatchesRemaining - 1).coerceAtLeast(0)
        )
    }

    private fun placeableJokerPieces(inventory: List<JokerType>): List<Piece> {
        return inventory.mapNotNull { jokerType ->
            jokerType.toPiece()
        }
    }

    private fun removeOneJoker(
        inventory: List<JokerType>,
        jokerType: JokerType
    ): List<JokerType> {
        var removed = false
        return inventory.filterNot { item ->
            if (!removed && item == jokerType) {
                removed = true
                true
            } else {
                false
            }
        }
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

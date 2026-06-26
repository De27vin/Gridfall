package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    @Test
    fun emptyBoardCreatesEightByEightGridFilledWithZeroes() {
        val board = Board.empty()

        assertEquals(Board.SIZE, board.cells.size)
        board.cells.forEach { row ->
            assertEquals(Board.SIZE, row.size)
            row.forEach { value ->
                assertEquals(0, value)
            }
        }
    }

    @Test
    fun canPlaceAllowsValidPlacement() {
        val piece = Piece("test", listOf(Cell(0, 0), Cell(0, 1)))

        assertTrue(GameEngine.canPlace(Board.empty(), piece, 2, 3))
    }

    @Test
    fun canPlaceRejectsOutOfBoundsPlacement() {
        val piece = Piece("test", listOf(Cell(0, 0), Cell(0, 1)))

        assertFalse(GameEngine.canPlace(Board.empty(), piece, 0, Board.SIZE - 1))
    }

    @Test
    fun canPlaceRejectsOccupiedCell() {
        val board = Board.empty().fill(2, 3)
        val piece = Piece("test", listOf(Cell(0, 0)))

        assertFalse(GameEngine.canPlace(board, piece, 2, 3))
    }

    @Test
    fun placePieceFillsBoardCellsAndUpdatesScore() {
        val piece = Piece("test", listOf(Cell(0, 0), Cell(0, 1)))
        val state = testState(currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 3)

        assertEquals(1, nextState.board.get(2, 3))
        assertEquals(1, nextState.board.get(2, 4))
        assertEquals(2, nextState.score)
    }

    @Test
    fun placePieceRejectsInvalidPlacementWithoutChangingState() {
        val piece = Piece("test", listOf(Cell(0, 0), Cell(0, 1)))
        val state = testState(currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(
            state = state,
            pieceIndex = 0,
            startRow = 0,
            startCol = Board.SIZE - 1
        )

        assertEquals(state, nextState)
    }

    @Test
    fun bombCanPlaceOnEmptyCell() {
        val bomb = bombPiece()
        val state = testState(currentPieces = listOf(bomb))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 3)

        assertEquals(0, nextState.board.get(2, 3))
        assertEquals(ScoreSystem.BOMB_PLACE_POINTS, nextState.score)
    }

    @Test
    fun bombCannotPlaceOutsideBoard() {
        val bomb = bombPiece()
        val state = testState(currentPieces = listOf(bomb))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = -1, startCol = 0)

        assertEquals(state, nextState)
    }

    @Test
    fun bombCannotPlaceOnOccupiedTargetCell() {
        val bomb = bombPiece()
        val state = testState(
            board = Board.empty().fill(2, 2),
            currentPieces = listOf(bomb)
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertEquals(state, nextState)
    }

    @Test
    fun bombClearsOccupiedCellsInThreeByThreeArea() {
        val bomb = bombPiece()
        val board = boardFromFilledCells(
            listOf(
                Cell(1, 1),
                Cell(1, 2),
                Cell(2, 1),
                Cell(3, 3),
                Cell(0, 0)
            )
        )
        val state = testState(board = board, currentPieces = listOf(bomb))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertEquals(0, nextState.board.get(1, 1))
        assertEquals(0, nextState.board.get(1, 2))
        assertEquals(0, nextState.board.get(2, 1))
        assertEquals(0, nextState.board.get(3, 3))
        assertEquals(1, nextState.board.get(0, 0))
        assertEquals(9, nextState.score)
    }

    @Test
    fun bombAtCornerOnlyClearsInsideBoardCells() {
        val bomb = bombPiece()
        val board = boardFromFilledCells(
            listOf(
                Cell(0, 1),
                Cell(1, 0),
                Cell(1, 1),
                Cell(2, 2)
            )
        )
        val state = testState(board = board, currentPieces = listOf(bomb))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertEquals(0, nextState.board.get(0, 0))
        assertEquals(0, nextState.board.get(0, 1))
        assertEquals(0, nextState.board.get(1, 0))
        assertEquals(0, nextState.board.get(1, 1))
        assertEquals(1, nextState.board.get(2, 2))
        assertEquals(7, nextState.score)
    }

    @Test
    fun bombDoesNotRemainOnBoardAfterExploding() {
        val bomb = bombPiece()
        val state = testState(currentPieces = listOf(bomb))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 4, startCol = 4)

        assertEquals(0, nextState.board.get(4, 4))
    }

    @Test
    fun calculateBombScoreUsesPlacePointAndClearedCellPoints() {
        val score = ScoreSystem.calculateBombScore(clearedCellCount = 4)

        assertEquals(9, score)
    }

    @Test
    fun clearLinesClearsFullRows() {
        val board = (0 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(3, col)
        }

        val result = GameEngine.clearLines(board)

        assertEquals(listOf(3), result.clearedRows)
        assertTrue(result.clearedColumns.isEmpty())
        (0 until Board.SIZE).forEach { col ->
            assertEquals(0, result.board.get(3, col))
        }
    }

    @Test
    fun clearLinesClearsFullColumns() {
        val board = (0 until Board.SIZE).fold(Board.empty()) { currentBoard, row ->
            currentBoard.fill(row, 4)
        }

        val result = GameEngine.clearLines(board)

        assertEquals(listOf(4), result.clearedColumns)
        assertTrue(result.clearedRows.isEmpty())
        (0 until Board.SIZE).forEach { row ->
            assertEquals(0, result.board.get(row, 4))
        }
    }

    @Test
    fun clearLinesHandlesSimultaneousRowsAndColumns() {
        val rowFilled = (0 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = (0 until Board.SIZE).fold(rowFilled) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }

        val result = GameEngine.clearLines(board)

        assertEquals(listOf(0), result.clearedRows)
        assertEquals(listOf(0), result.clearedColumns)
        (0 until Board.SIZE).forEach { index ->
            assertEquals(0, result.board.get(0, index))
            assertEquals(0, result.board.get(index, 0))
        }
    }

    @Test
    fun calculateMoveScoreAppliesCellLineMultiLineAndComboPoints() {
        val score = ScoreSystem.calculateMoveScore(
            placedCellCount = 4,
            clearedLineCount = 2,
            previousCombo = 1
        )

        assertEquals(54, score)
    }

    @Test
    fun levelForScoreReturnsExpectedLevels() {
        assertEquals(1, LevelSystem.levelForScore(0))
        assertEquals(1, LevelSystem.levelForScore(59))
        assertEquals(2, LevelSystem.levelForScore(60))
        assertEquals(3, LevelSystem.levelForScore(150))
        assertEquals(4, LevelSystem.levelForScore(300))
        assertEquals(5, LevelSystem.levelForScore(550))
        assertEquals(6, LevelSystem.levelForScore(900))
        assertEquals(7, LevelSystem.levelForScore(1400))
        assertEquals(8, LevelSystem.levelForScore(2000))
    }

    @Test
    fun nextLevelScoreReturnsExpectedThresholds() {
        assertEquals(60, LevelSystem.nextLevelScore(1))
        assertEquals(150, LevelSystem.nextLevelScore(2))
        assertEquals(300, LevelSystem.nextLevelScore(3))
        assertEquals(550, LevelSystem.nextLevelScore(4))
        assertEquals(900, LevelSystem.nextLevelScore(5))
        assertEquals(1400, LevelSystem.nextLevelScore(6))
        assertEquals(2000, LevelSystem.nextLevelScore(7))
        assertNull(LevelSystem.nextLevelScore(8))
    }

    @Test
    fun initialStateStartsAtLevelOneByDerivedScore() {
        val state = GameEngine.createInitialState()

        assertEquals(0, state.score)
        assertEquals(1, LevelSystem.levelForScore(state.score))
    }

    @Test
    fun placePieceUpdatesComboAfterLineClear() {
        val board = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val piece = Piece("test", listOf(Cell(0, 0)))
        val state = testState(
            board = board,
            currentPieces = listOf(piece),
            combo = 1
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)

        assertEquals(2, nextState.combo)
        assertEquals(16, nextState.score)
    }

    @Test
    fun newBatchGeneratesAfterAllPiecesAreUsed() {
        val pieces = listOf(
            Piece("a", listOf(Cell(0, 0))),
            Piece("b", listOf(Cell(0, 0))),
            Piece("c", listOf(Cell(0, 0)))
        )
        val state = testState(currentPieces = pieces)

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 2)

        assertTrue(afterThird.usedPieceIndices.isEmpty())
        assertEquals(3, afterThird.currentPieces.size)
        assertNotEquals(setOf(0, 1, 2), afterThird.usedPieceIndices)
    }

    @Test
    fun hasAnyValidMoveReturnsFalseWhenNoPieceCanFit() {
        val board = boardFromFilledCells(
            filledCells = (0 until Board.SIZE).flatMap { row ->
                (0 until Board.SIZE).map { col -> Cell(row, col) }
            }
        )
        val piece = Piece("single", listOf(Cell(0, 0)))

        assertFalse(GameEngine.hasAnyValidMove(board, listOf(piece)))
    }

    @Test
    fun hasAnyValidMoveAllowsBombWhenAnyEmptyCellExists() {
        val board = boardWithZeroes(zeroes = setOf(Cell(3, 3)))

        assertTrue(GameEngine.hasAnyValidMove(board, listOf(bombPiece())))
    }

    @Test
    fun placePieceDetectsGameOverForRemainingUnavailablePieces() {
        val board = boardWithZeroes(
            zeroes = setOf(
                Cell(0, 0),
                Cell(0, 1),
                Cell(1, 0),
                Cell(1, 1),
                Cell(2, 2),
                Cell(3, 3),
                Cell(4, 4),
                Cell(5, 5),
                Cell(6, 6),
                Cell(7, 7)
            )
        )
        val single = Piece("single", listOf(Cell(0, 0)))
        val square = Piece(
            id = "square",
            cells = listOf(
                Cell(0, 0), Cell(0, 1),
                Cell(1, 0), Cell(1, 1)
            )
        )
        val state = testState(
            board = board,
            currentPieces = listOf(single, square)
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertTrue(nextState.isGameOver)
    }

    @Test
    fun createInitialStateOffersContract() {
        val state = GameEngine.createInitialState()

        assertNotNull(state.contractState.offeredContract)
        assertNull(state.contractState.activeContract)
    }

    @Test
    fun acceptContractActivatesOfferedContract() {
        val contract = testContract(ContractType.ClearAtLeastOneLine)
        val state = testState(
            currentPieces = listOf(Piece("single", listOf(Cell(0, 0)))),
            contractState = ContractState(offeredContract = contract)
        )

        val nextState = GameEngine.acceptContract(state)

        assertEquals(contract, nextState.contractState.activeContract)
        assertNull(nextState.contractState.offeredContract)
        assertTrue(nextState.contractState.isAccepted)
    }

    @Test
    fun skipContractClearsOfferedContract() {
        val contract = testContract(ContractType.ClearAtLeastOneLine)
        val state = testState(
            currentPieces = listOf(Piece("single", listOf(Cell(0, 0)))),
            contractState = ContractState(offeredContract = contract)
        )

        val nextState = GameEngine.skipContract(state)

        assertNull(nextState.contractState.offeredContract)
        assertNull(nextState.contractState.activeContract)
        assertFalse(nextState.contractState.isAccepted)
    }

    @Test
    fun acceptedContractTracksProgressDuringBatch() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.ScoreAtLeastTwenty)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 1, startCol = 1)

        assertEquals(1, nextState.contractState.batchPlacedPieces)
        assertEquals(1, nextState.contractState.batchScoreGained)
        assertFalse(nextState.contractState.usedEdge)
        assertFalse(nextState.contractState.usedCenter)
    }

    @Test
    fun bombCountsAsOnePlacedPieceForContracts() {
        val bomb = bombPiece()
        val board = boardFromFilledCells(
            listOf(
                Cell(1, 1),
                Cell(1, 2),
                Cell(2, 1),
                Cell(3, 3)
            )
        )
        val contract = testContract(ContractType.ScoreAtLeastTwenty)
        val state = acceptedContractState(
            contract = contract,
            board = board,
            currentPieces = listOf(bomb, Piece("single", listOf(Cell(0, 0))))
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertEquals(1, nextState.contractState.batchPlacedPieces)
        assertEquals(9, nextState.contractState.batchScoreGained)
        assertTrue(nextState.contractState.usedCenter)
    }

    @Test
    fun bombClearingDoesNotCountAsLineClearContractProgress() {
        val bomb = bombPiece()
        val board = boardFromFilledCells(
            listOf(
                Cell(1, 1),
                Cell(1, 2),
                Cell(2, 1),
                Cell(2, 3),
                Cell(3, 3)
            )
        )
        val contract = testContract(ContractType.ClearAtLeastOneLine)
        val state = acceptedContractState(
            contract = contract,
            board = board,
            currentPieces = listOf(bomb, Piece("single", listOf(Cell(0, 0))))
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertEquals(0, nextState.contractState.batchClearedLines)
        assertEquals(1, nextState.contractState.batchPlacedPieces)
    }

    @Test
    fun clearAtLeastOneLineContractCompletesAtBatchEnd() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val board = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val contract = testContract(ContractType.ClearAtLeastOneLine, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            board = board,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 2)

        assertTrue(afterThird.contractState.isCompleted)
        assertEquals(contract, afterThird.contractState.resolvedContract)
        assertEquals(38, afterThird.score)
    }

    @Test
    fun clearExactlyTwoLinesContractCompletesAtBatchEnd() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val contract = testContract(ContractType.ClearExactlyTwoLines, rewardPoints = 35)
        val state = acceptedContractState(
            contract = contract,
            board = board,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 2)

        assertTrue(afterThird.contractState.isCompleted)
        assertEquals(contract, afterThird.contractState.resolvedContract)
        assertEquals(83, afterThird.score)
    }

    @Test
    fun noEdgePlacementContractCompletesWhenNoPlacedCellTouchesEdge() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 1, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 2)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 3)

        assertTrue(afterThird.contractState.isCompleted)
        assertFalse(afterThird.contractState.isFailed)
        assertEquals(28, afterThird.score)
    }

    @Test
    fun noEdgePlacementContractFailsWhenPlacedCellTouchesEdge() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 2)

        assertFalse(afterThird.contractState.isCompleted)
        assertTrue(afterThird.contractState.isFailed)
        assertEquals(3, afterThird.score)
    }

    @Test
    fun avoidCenterAreaContractCompletesWhenCenterFourByFourIsUnused() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.AvoidCenterArea, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 2)

        assertTrue(afterThird.contractState.isCompleted)
        assertFalse(afterThird.contractState.isFailed)
        assertEquals(28, afterThird.score)
    }

    @Test
    fun avoidCenterAreaContractFailsWhenPlacedCellUsesCenterFourByFour() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.AvoidCenterArea, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 0)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 1)

        assertFalse(afterThird.contractState.isCompleted)
        assertTrue(afterThird.contractState.isFailed)
        assertEquals(3, afterThird.score)
    }

    @Test
    fun scoreAtLeastTwentyContractCompletesFromBatchScoreOnly() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val contract = testContract(ContractType.ScoreAtLeastTwenty, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            board = board,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 2)

        assertTrue(afterThird.contractState.isCompleted)
        assertEquals(73, afterThird.score)
        assertEquals(0, afterThird.contractState.batchScoreGained)
    }

    @Test
    fun failedContractDoesNotAddRewardPoints() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.ScoreAtLeastTwenty, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 2)

        assertTrue(afterThird.contractState.isFailed)
        assertFalse(afterThird.contractState.rewardClaimed)
        assertEquals(3, afterThird.score)
    }

    @Test
    fun completedContractRewardIsNotAddedAgainAfterBatchEnd() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 1, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 2)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 3)
        val afterInvalidMove = GameEngine.placePiece(afterThird, pieceIndex = 0, startRow = -1, startCol = 0)

        assertTrue(afterThird.contractState.rewardClaimed)
        assertNull(afterThird.contractState.activeContract)
        assertEquals(afterThird.score, afterInvalidMove.score)
    }

    @Test
    fun newContractIsOfferedAfterBatchEnds() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 1, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 2)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 3)

        assertNotNull(afterThird.contractState.offeredContract)
        assertNull(afterThird.contractState.activeContract)
        assertEquals(0, afterThird.contractState.batchPlacedPieces)
    }

    @Test
    fun generatedBatchContainsAtMostOneBomb() {
        repeat(100) { seed ->
            val batch = PieceGenerator.generateBatch(
                availablePieces = PieceLibrary.starterPieces + PieceLibrary.bombPiece,
                bombChance = 1f,
                random = Random(seed)
            )

            assertEquals(1, batch.count { it.effect == PieceEffect.Bomb })
        }
    }

    @Test
    fun higherLevelGenerationMakesLargerPiecesMoreCommon() {
        val smallPiece = Piece("small", listOf(Cell(0, 0)))
        val largePiece = Piece(
            id = "large",
            cells = listOf(
                Cell(0, 0), Cell(0, 1),
                Cell(1, 0), Cell(1, 1)
            )
        )
        val lowLevelLargeCount = generatedLargePieceCount(
            level = 1,
            pieces = listOf(smallPiece, largePiece)
        )
        val highLevelLargeCount = generatedLargePieceCount(
            level = 6,
            pieces = listOf(smallPiece, largePiece)
        )

        assertTrue(highLevelLargeCount > lowLevelLargeCount)
    }

    @Test
    fun contractRewardScalesWithLevel() {
        val lowLevelContract = ContractGenerator.generate(level = 1)
        val highLevelContract = ContractGenerator.generate(level = 4)

        assertTrue(lowLevelContract.rewardPoints in setOf(25, 35))
        assertTrue(highLevelContract.rewardPoints in setOf(40, 50))
    }

    @Test
    fun restartReturnsToLevelOneByDerivedScore() {
        val restartedState = GameEngine.createInitialState()

        assertEquals(1, LevelSystem.levelForScore(restartedState.score))
    }

    private fun testState(
        board: Board = Board.empty(),
        currentPieces: List<Piece>,
        usedPieceIndices: Set<Int> = emptySet(),
        score: Int = 0,
        combo: Int = 0,
        isGameOver: Boolean = false,
        contractState: ContractState = ContractState()
    ): GameState {
        return GameState(
            board = board,
            currentPieces = currentPieces,
            usedPieceIndices = usedPieceIndices,
            score = score,
            combo = combo,
            isGameOver = isGameOver,
            contractState = contractState
        )
    }

    private fun acceptedContractState(
        contract: Contract,
        board: Board = Board.empty(),
        currentPieces: List<Piece>
    ): GameState {
        return testState(
            board = board,
            currentPieces = currentPieces,
            contractState = ContractState(
                activeContract = contract,
                isAccepted = true
            )
        )
    }

    private fun testContract(
        type: ContractType,
        rewardPoints: Int = 25
    ): Contract {
        return Contract(
            id = type.name,
            title = type.name,
            description = type.name,
            rewardPoints = rewardPoints,
            type = type
        )
    }

    private fun bombPiece(): Piece {
        return Piece(
            id = "bomb_single",
            cells = listOf(Cell(0, 0)),
            effect = PieceEffect.Bomb
        )
    }

    private fun generatedLargePieceCount(
        level: Int,
        pieces: List<Piece>
    ): Int {
        return (0 until 300).sumOf { seed ->
            PieceGenerator.generateBatch(
                level = level,
                availablePieces = pieces,
                bombChance = 0f,
                random = Random(seed)
            ).count { piece -> piece.cells.size >= 4 }
        }
    }

    private fun boardFromFilledCells(filledCells: List<Cell>): Board {
        return filledCells.fold(Board.empty()) { board, cell ->
            board.fill(cell.row, cell.col)
        }
    }

    private fun boardWithZeroes(zeroes: Set<Cell>): Board {
        val cells = List(Board.SIZE) { row ->
            List(Board.SIZE) { col ->
                if (Cell(row, col) in zeroes) 0 else 1
            }
        }

        return Board(cells)
    }
}

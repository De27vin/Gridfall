package com.example.gridfall.game

import com.example.gridfall.ui.centerWarningCells
import com.example.gridfall.ui.contractWarningCells
import com.example.gridfall.ui.edgeWarningCells
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
    fun emptyBoardReportsNoFilledCells() {
        assertTrue(Board.empty().isEmpty())
    }

    @Test
    fun boardWithOneFilledCellDoesNotReportEmpty() {
        assertFalse(Board.empty().fill(3, 4).isEmpty())
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
    fun megaBombAffectedCellsCenterAroundMiddleHover() {
        assertEquals(
            expectedSquareCells(rowRange = 3..6, colRange = 3..6),
            GameEngine.megaBombAffectedCells(row = 4, col = 4)
        )
    }

    @Test
    fun megaBombAffectedCellsClampAtTopLeftCorner() {
        assertEquals(
            expectedSquareCells(rowRange = 0..3, colRange = 0..3),
            GameEngine.megaBombAffectedCells(row = 0, col = 0)
        )
    }

    @Test
    fun megaBombAffectedCellsClampAtBottomRightCorner() {
        assertEquals(
            expectedSquareCells(rowRange = 4..7, colRange = 4..7),
            GameEngine.megaBombAffectedCells(row = 7, col = 7)
        )
    }

    @Test
    fun megaBombAffectedCellsClampNearTopLeftEdge() {
        assertEquals(
            expectedSquareCells(rowRange = 0..3, colRange = 0..3),
            GameEngine.megaBombAffectedCells(row = 1, col = 1)
        )
    }

    @Test
    fun megaBombAffectedCellsClampNearBottomRightEdge() {
        assertEquals(
            expectedSquareCells(rowRange = 4..7, colRange = 4..7),
            GameEngine.megaBombAffectedCells(row = 6, col = 6)
        )
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
    fun calculateMoveScoreWithoutLineClearDoesNotAddCrossClearBonus() {
        val score = ScoreSystem.calculateMoveScore(
            placedCellCount = 3,
            clearedLineCount = 0,
            previousCombo = 2
        )

        assertEquals(3, score)
    }

    @Test
    fun calculateComboBonusUsesStreakTableWithCap() {
        assertEquals(0, ScoreSystem.calculateComboBonus(1))
        assertEquals(10, ScoreSystem.calculateComboBonus(2))
        assertEquals(20, ScoreSystem.calculateComboBonus(3))
        assertEquals(40, ScoreSystem.calculateComboBonus(4))
        assertEquals(80, ScoreSystem.calculateComboBonus(5))
        assertEquals(160, ScoreSystem.calculateComboBonus(6))
        assertEquals(160, ScoreSystem.calculateComboBonus(7))
    }

    @Test
    fun calculateMoveScoreAppliesComboBonusFromNextStreak() {
        assertEquals(11, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 0))
        assertEquals(21, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 1))
        assertEquals(31, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 2))
        assertEquals(51, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 3))
        assertEquals(91, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 4))
        assertEquals(171, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 5))
        assertEquals(171, ScoreSystem.calculateMoveScore(placedCellCount = 1, clearedLineCount = 1, previousCombo = 6))
    }

    @Test
    fun calculateMultiLineBonusScalesByTotalClearedLines() {
        assertEquals(0, ScoreSystem.calculateMultiLineBonus(1))
        assertEquals(20, ScoreSystem.calculateMultiLineBonus(2))
        assertEquals(40, ScoreSystem.calculateMultiLineBonus(3))
        assertEquals(60, ScoreSystem.calculateMultiLineBonus(4))
    }

    @Test
    fun calculateMoveScoreAddsPerfectClearBonus() {
        val score = ScoreSystem.calculateMoveScore(
            placedCellCount = 1,
            clearedLineCount = 1,
            previousCombo = 0,
            isPerfectClear = true
        )

        assertEquals(211, score)
    }

    @Test
    fun rowOnlyClearDoesNotAddCrossClearBonus() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = rowReady.fill(2, 2)
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)

        assertEquals(11, nextState.score)
    }

    @Test
    fun columnOnlyClearDoesNotAddCrossClearBonus() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val columnReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val board = columnReady.fill(2, 2)
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertEquals(11, nextState.score)
    }

    @Test
    fun simultaneousRowAndColumnClearAddsCrossClearAndPerfectClearBonuses() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertTrue(nextState.board.isEmpty())
        assertEquals(241, nextState.score)
    }

    @Test
    fun multipleRowsAndOneColumnClearScalesMultiLineBonus() {
        val piece = Piece("vertical_two", listOf(Cell(0, 0), Cell(1, 0)))
        val rowZeroReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val rowsReady = (1 until Board.SIZE).fold(rowZeroReady) { currentBoard, col ->
            currentBoard.fill(1, col)
        }
        val board = (2 until Board.SIZE).fold(rowsReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertEquals(272, nextState.score)
    }

    @Test
    fun oneRowAndMultipleColumnsClearScalesMultiLineBonus() {
        val piece = Piece("horizontal_two", listOf(Cell(0, 0), Cell(0, 1)))
        val rowReady = (2 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val firstColumnReady = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val board = (1 until Board.SIZE).fold(firstColumnReady) { currentBoard, row ->
            currentBoard.fill(row, 1)
        }
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertEquals(272, nextState.score)
    }

    @Test
    fun bombClearDoesNotAddCrossClearBonus() {
        val bomb = bombPiece()
        val rowReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val state = testState(board = board, currentPieces = listOf(bomb))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertEquals(5, nextState.score)
    }

    @Test
    fun normalMovePerfectClearAddsBonus() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val board = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)

        assertTrue(nextState.board.isEmpty())
        assertEquals(211, nextState.score)
    }

    @Test
    fun lineClearThatLeavesCellsBehindDoesNotAddPerfectClearBonus() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = rowReady.fill(2, 2)
        val state = testState(board = board, currentPieces = listOf(piece))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)

        assertFalse(nextState.board.isEmpty())
        assertEquals(11, nextState.score)
    }

    @Test
    fun bombPerfectClearAddsBonus() {
        val bomb = bombPiece()
        val state = testState(
            board = Board.empty().fill(0, 1),
            currentPieces = listOf(bomb)
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertTrue(nextState.board.isEmpty())
        assertEquals(203, nextState.score)
    }

    @Test
    fun bombClearThatLeavesCellsBehindDoesNotAddPerfectClearBonus() {
        val bomb = bombPiece()
        val state = testState(
            board = Board.empty()
                .fill(0, 1)
                .fill(7, 7),
            currentPieces = listOf(bomb)
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertFalse(nextState.board.isEmpty())
        assertEquals(3, nextState.score)
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
        assertEquals(8, LevelSystem.levelForScore(2749))
        assertEquals(9, LevelSystem.levelForScore(2750))
        assertEquals(10, LevelSystem.levelForScore(3650))
        assertEquals(11, LevelSystem.levelForScore(4700))
        assertEquals(12, LevelSystem.levelForScore(5900))
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
        assertEquals(2750, LevelSystem.nextLevelScore(8))
        assertEquals(3650, LevelSystem.nextLevelScore(9))
        assertEquals(4700, LevelSystem.nextLevelScore(10))
        assertEquals(5900, LevelSystem.nextLevelScore(11))
    }

    @Test
    fun thresholdForLevelSupportsEndlessLevelsAfterLevelEight() {
        assertEquals(2000, LevelSystem.thresholdForLevel(8))
        assertEquals(2750, LevelSystem.thresholdForLevel(9))
        assertEquals(3650, LevelSystem.thresholdForLevel(10))
        assertEquals(4700, LevelSystem.thresholdForLevel(11))
        assertEquals(5900, LevelSystem.thresholdForLevel(12))
        assertTrue(LevelSystem.thresholdForLevel(30) > LevelSystem.thresholdForLevel(29))
    }

    @Test
    fun levelProgressAfterLevelEightStaysBelowFullUntilNextThreshold() {
        val level = LevelSystem.levelForScore(3200)
        val currentLevelStart = LevelSystem.thresholdForLevel(level)
        val nextLevelScore = LevelSystem.nextLevelScore(level)
        val progress = (3200 - currentLevelStart).toFloat() / (nextLevelScore - currentLevelStart)

        assertEquals(9, level)
        assertTrue(progress > 0f)
        assertTrue(progress < 1f)
    }

    @Test
    fun levelEightHasNoMaxLevelState() {
        assertEquals(2750, LevelSystem.nextLevelScore(8))
        assertTrue(LevelSystem.nextLevelScore(8) > LevelSystem.thresholdForLevel(8))
    }

    @Test
    fun initialStateStartsAtLevelOneByDerivedScore() {
        val state = GameEngine.createInitialState()

        assertEquals(0, state.score)
        assertEquals(1, LevelSystem.levelForScore(state.score))
    }

    @Test
    fun placePieceUpdatesComboAfterLineClear() {
        val rowReady = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = rowReady.fill(2, 2)
        val piece = Piece("test", listOf(Cell(0, 0)))
        val state = testState(
            board = board,
            currentPieces = listOf(piece),
            combo = 1
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)

        assertEquals(2, nextState.combo)
        assertEquals(21, nextState.score)
    }

    @Test
    fun placePieceResetsComboAfterNonClearingMove() {
        val piece = Piece("test", listOf(Cell(0, 0)))
        val state = testState(
            currentPieces = listOf(piece),
            combo = 4
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertEquals(0, nextState.combo)
        assertEquals(1, nextState.score)
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
    fun initialStateHasNoImmediateContractOffer() {
        val state = GameEngine.createInitialState()

        assertNull(state.contractState.offeredContract)
        assertNull(state.contractState.activeContract)
        assertTrue(state.contractState.batchesUntilNextOffer in 3..5)
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
        assertTrue(nextState.contractState.batchesUntilNextOffer in 12..18)
    }

    @Test
    fun contractOfferAppearsOnlyAfterCooldownBatches() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val cooldownTwoState = testState(
            currentPieces = listOf(piece, piece, piece),
            contractState = ContractState(batchesUntilNextOffer = 2)
        )
        val cooldownOneState = testState(
            currentPieces = listOf(piece, piece, piece),
            contractState = ContractState(batchesUntilNextOffer = 1)
        )

        val afterCooldownTwoBatch = placeAllThreeSingles(cooldownTwoState)
        val afterCooldownOneBatch = placeAllThreeSingles(cooldownOneState)

        assertNull(afterCooldownTwoBatch.contractState.offeredContract)
        assertEquals(1, afterCooldownTwoBatch.contractState.batchesUntilNextOffer)
        assertNotNull(afterCooldownOneBatch.contractState.offeredContract)
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
                Cell(3, 3),
                Cell(0, 0)
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
        val rowReady = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val board = rowReady.fill(2, 2)
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
        val crossReady = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val board = crossReady.fill(2, 2)
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
        assertEquals(78, afterThird.score)
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

        assertNotNull(afterFirst.contractState.activeContract)
        assertFalse(afterFirst.contractState.isFailed)
        assertTrue(afterThird.contractState.isCompleted)
        assertFalse(afterThird.contractState.isFailed)
        assertEquals(28, afterThird.score)
    }

    @Test
    fun noEdgePlacementContractFailsImmediatelyWhenPlacedCellTouchesEdge() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 1)

        assertNull(afterFirst.contractState.activeContract)
        assertEquals(contract, afterFirst.contractState.resolvedContract)
        assertFalse(afterFirst.contractState.isCompleted)
        assertTrue(afterFirst.contractState.isFailed)
        assertTrue(afterFirst.contractState.penaltyApplied)
        assertTrue(afterFirst.contractState.batchesUntilNextOffer in 12..18)
        assertTrue(contractWarningCells(afterFirst.contractState).isEmpty())
        assertEquals(0, afterFirst.score)
    }

    @Test
    fun noEdgePlacementImmediateFailureAppliesPenaltyAndDoesNotApplyAgainAtBatchEnd() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            score = 80,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 2)

        assertEquals(31, afterFirst.score)
        assertNull(afterFirst.contractState.activeContract)
        assertTrue(afterFirst.contractState.penaltyApplied)
        assertEquals(33, afterThird.score)
    }

    @Test
    fun noEdgePlacementImmediateFailureClampsScoreAtZero() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 1)

        assertEquals(0, afterFirst.score)
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

        assertNotNull(afterFirst.contractState.activeContract)
        assertFalse(afterFirst.contractState.isFailed)
        assertTrue(afterThird.contractState.isCompleted)
        assertFalse(afterThird.contractState.isFailed)
        assertEquals(28, afterThird.score)
    }

    @Test
    fun avoidCenterAreaContractFailsImmediatelyWhenPlacedCellUsesCenterFourByFour() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.AvoidCenterArea, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertNull(afterFirst.contractState.activeContract)
        assertEquals(contract, afterFirst.contractState.resolvedContract)
        assertFalse(afterFirst.contractState.isCompleted)
        assertTrue(afterFirst.contractState.isFailed)
        assertTrue(afterFirst.contractState.penaltyApplied)
        assertTrue(afterFirst.contractState.batchesUntilNextOffer in 12..18)
        assertTrue(contractWarningCells(afterFirst.contractState).isEmpty())
        assertEquals(0, afterFirst.score)
    }

    @Test
    fun avoidCenterAreaImmediateFailureAppliesPenaltyAndDoesNotApplyAgainAtBatchEnd() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.AvoidCenterArea, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            score = 80,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 0)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 1)

        assertEquals(31, afterFirst.score)
        assertNull(afterFirst.contractState.activeContract)
        assertTrue(afterFirst.contractState.penaltyApplied)
        assertEquals(33, afterThird.score)
    }

    @Test
    fun scoreAtLeastTwentyContractCompletesFromBatchScoreOnly() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (1 until Board.SIZE).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val crossReady = (1 until Board.SIZE).fold(rowReady) { currentBoard, row ->
            currentBoard.fill(row, 0)
        }
        val board = crossReady.fill(2, 2)
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
        assertEquals(68, afterThird.score)
        assertEquals(0, afterThird.contractState.batchScoreGained)
    }

    @Test
    fun perfectClearBonusCountsTowardScoreAtLeastTwentyContractProgress() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val board = (0 until Board.SIZE - 1).fold(Board.empty()) { currentBoard, col ->
            currentBoard.fill(0, col)
        }
        val contract = testContract(ContractType.ScoreAtLeastTwenty, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            board = board,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 7)

        assertEquals(211, afterFirst.score)
        assertEquals(211, afterFirst.contractState.batchScoreGained)
    }

    @Test
    fun failedContractSubtractsPenaltyPoints() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.ScoreAtLeastTwenty, rewardPoints = 25)
        val state = acceptedContractState(
            contract = contract,
            score = 80,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 2)

        assertTrue(afterThird.contractState.isFailed)
        assertFalse(afterThird.contractState.rewardClaimed)
        assertTrue(afterThird.contractState.penaltyApplied)
        assertEquals(33, afterThird.score)
    }

    @Test
    fun failedContractCannotReduceScoreBelowZero() {
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
        assertEquals(0, afterThird.score)
    }

    @Test
    fun skippedContractDoesNotSubtractScore() {
        val contract = testContract(ContractType.ScoreAtLeastTwenty, rewardPoints = 25)
        val state = testState(
            currentPieces = listOf(Piece("single", listOf(Cell(0, 0)))),
            score = 40,
            contractState = ContractState(offeredContract = contract)
        )

        val nextState = GameEngine.skipContract(state)

        assertEquals(40, nextState.score)
        assertNull(nextState.contractState.offeredContract)
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
    fun newContractIsNotOfferedImmediatelyAfterContractResolution() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement)
        val state = acceptedContractState(
            contract = contract,
            currentPieces = listOf(piece, piece, piece)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 1, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 2)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 3)

        assertNull(afterThird.contractState.offeredContract)
        assertNull(afterThird.contractState.activeContract)
        assertEquals(0, afterThird.contractState.batchPlacedPieces)
        assertTrue(afterThird.contractState.batchesUntilNextOffer in 12..18)
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
    fun normalBatchGenerationNeverProducesMegaBomb() {
        val megaBombPiece = JokerType.MegaBomb.toPiece() ?: error("Mega Bomb should create a piece")
        repeat(100) { seed ->
            val batch = PieceGenerator.generateBatch(
                availablePieces = PieceLibrary.starterPieces + megaBombPiece,
                bombChance = 0f,
                random = Random(seed)
            )

            assertTrue(batch.none { it.effect == PieceEffect.MegaBomb })
        }
    }

    @Test
    fun pieceRarityDefaultsToCommon() {
        val piece = Piece("default", listOf(Cell(0, 0)))

        assertEquals(PieceRarity.Common, piece.rarity)
    }

    @Test
    fun pieceHelperPropertiesReportBoundsAndCellCount() {
        val piece = Piece(
            id = "helpers",
            cells = listOf(Cell(0, 1), Cell(1, 1), Cell(2, 0), Cell(2, 1))
        )

        assertEquals(2, piece.width)
        assertEquals(3, piece.height)
        assertEquals(4, piece.cellCount)
    }

    @Test
    fun expandedPieceLibraryContainsRemainingNewShapes() {
        val ids = PieceLibrary.starterPieces.map { it.id }.toSet()

        assertTrue(ids.containsAll(
            setOf(
                "plus_5",
                "big_l_5",
                "big_l_5_mirrored",
                "t_4",
                "t_4_down",
                "s_4",
                "z_4"
            )
        ))
    }

    @Test
    fun overlyDifficultPiecesAreRemovedFromLibrary() {
        val ids = PieceLibrary.starterPieces.map { it.id }.toSet()

        assertFalse(ids.contains("horizontal_5"))
        assertFalse(ids.contains("vertical_5"))
        assertFalse(ids.contains("square_3x3"))
    }

    @Test
    fun allLibraryPieceIdsAreUnique() {
        val ids = (PieceLibrary.starterPieces + PieceLibrary.bombPiece).map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun allLibraryPiecesHaveCells() {
        (PieceLibrary.starterPieces + PieceLibrary.bombPiece).forEach { piece ->
            assertTrue(piece.cells.isNotEmpty())
        }
    }

    @Test
    fun allLibraryPieceCellsAreNonNegative() {
        (PieceLibrary.starterPieces + PieceLibrary.bombPiece).flatMap { it.cells }.forEach { cell ->
            assertTrue(cell.row >= 0)
            assertTrue(cell.col >= 0)
        }
    }

    @Test
    fun fourCellLPiecesAreEpic() {
        val piecesById = PieceLibrary.starterPieces.associateBy { it.id }
        val lPiece = piecesById.getValue("l_4")
        val mirroredLPiece = piecesById.getValue("l_4_mirrored")

        assertEquals(PieceRarity.Epic, lPiece.rarity)
        assertEquals(PieceRarity.Epic, mirroredLPiece.rarity)
        assertEquals(4, lPiece.cellCount)
        assertEquals(4, mirroredLPiece.cellCount)
    }

    @Test
    fun bombIsRareAndKeepsBombEffect() {
        assertEquals(PieceRarity.Rare, PieceLibrary.bombPiece.rarity)
        assertEquals(PieceEffect.Bomb, PieceLibrary.bombPiece.effect)
    }

    @Test
    fun levelOneGenerationDoesNotProduceEpicPieces() {
        repeat(200) { seed ->
            val batch = PieceGenerator.generateBatch(
                level = 1,
                bombChance = 0f,
                random = Random(seed)
            )

            assertTrue(batch.none { it.rarity == PieceRarity.Epic })
        }
    }

    @Test
    fun highLevelGenerationCanProduceRareAndEpicPieces() {
        val generated = (0 until 500).flatMap { seed ->
            PieceGenerator.generateBatch(
                level = 6,
                bombChance = 0f,
                random = Random(seed)
            )
        }

        assertTrue(generated.any { it.rarity == PieceRarity.Rare })
        assertTrue(generated.any { it.rarity == PieceRarity.Epic })
    }

    @Test
    fun weightedGenerationFavorsCommonPieces() {
        val generated = (0 until 600).flatMap { seed ->
            PieceGenerator.generateBatch(
                level = 4,
                bombChance = 0f,
                random = Random(seed)
            )
        }
        val commonCount = generated.count { it.rarity == PieceRarity.Common }
        val rareAndEpicCount = generated.count { it.rarity == PieceRarity.Rare || it.rarity == PieceRarity.Epic }

        assertTrue(commonCount > rareAndEpicCount)
    }

    @Test
    fun rarityWeightsKeepEarlyLevelBalance() {
        assertEquals(
            mapOf(
                PieceRarity.Common to 75,
                PieceRarity.Uncommon to 22,
                PieceRarity.Rare to 3,
                PieceRarity.Epic to 0
            ),
            PieceGenerator.rarityWeightsForLevel(1)
        )
        assertEquals(
            mapOf(
                PieceRarity.Common to 60,
                PieceRarity.Uncommon to 28,
                PieceRarity.Rare to 10,
                PieceRarity.Epic to 2
            ),
            PieceGenerator.rarityWeightsForLevel(3)
        )
        assertEquals(
            mapOf(
                PieceRarity.Common to 48,
                PieceRarity.Uncommon to 32,
                PieceRarity.Rare to 15,
                PieceRarity.Epic to 5
            ),
            PieceGenerator.rarityWeightsForLevel(6)
        )
    }

    @Test
    fun rarityWeightsScaleProgressivelyAfterLevelEight() {
        val levelSix = PieceGenerator.rarityWeightsForLevel(6)
        val levelEight = PieceGenerator.rarityWeightsForLevel(8)
        val levelTen = PieceGenerator.rarityWeightsForLevel(10)
        val levelTwelve = PieceGenerator.rarityWeightsForLevel(12)

        assertEquals(45, levelEight.getValue(PieceRarity.Common))
        assertEquals(32, levelEight.getValue(PieceRarity.Uncommon))
        assertEquals(17, levelEight.getValue(PieceRarity.Rare))
        assertEquals(6, levelEight.getValue(PieceRarity.Epic))

        assertEquals(40, levelTen.getValue(PieceRarity.Common))
        assertEquals(20, levelTen.getValue(PieceRarity.Rare))
        assertEquals(8, levelTen.getValue(PieceRarity.Epic))

        assertEquals(35, levelTwelve.getValue(PieceRarity.Common))
        assertEquals(23, levelTwelve.getValue(PieceRarity.Rare))
        assertEquals(11, levelTwelve.getValue(PieceRarity.Epic))

        assertTrue(levelEight.getValue(PieceRarity.Rare) > levelSix.getValue(PieceRarity.Rare))
        assertTrue(levelEight.getValue(PieceRarity.Epic) > levelSix.getValue(PieceRarity.Epic))
        assertTrue(levelTen.getValue(PieceRarity.Rare) > levelEight.getValue(PieceRarity.Rare))
        assertTrue(levelTen.getValue(PieceRarity.Epic) > levelEight.getValue(PieceRarity.Epic))
        assertTrue(levelTwelve.getValue(PieceRarity.Rare) > levelTen.getValue(PieceRarity.Rare))
        assertTrue(levelTwelve.getValue(PieceRarity.Epic) > levelTen.getValue(PieceRarity.Epic))
    }

    @Test
    fun highLevelRarityWeightsAreCappedAndPositive() {
        val highLevelWeights = PieceGenerator.rarityWeightsForLevel(40)

        assertTrue(highLevelWeights.values.all { it >= 0 })
        assertTrue(highLevelWeights.values.sum() > 0)
        assertTrue(highLevelWeights.getValue(PieceRarity.Common) >= 25)
        assertTrue(highLevelWeights.getValue(PieceRarity.Uncommon) in 28..32)
        assertTrue(highLevelWeights.getValue(PieceRarity.Rare) <= 30)
        assertTrue(highLevelWeights.getValue(PieceRarity.Epic) <= 17)
    }

    @Test
    fun generateBatchReturnsThreePiecesByDefault() {
        val batch = PieceGenerator.generateBatch(random = Random(7), bombChance = 0f)

        assertEquals(3, batch.size)
    }

    @Test
    fun generateBatchNeverReturnsRemovedPieces() {
        val removedIds = setOf("horizontal_5", "vertical_5", "square_3x3")
        val generatedIds = (0 until 500).flatMap { seed ->
            PieceGenerator.generateBatch(
                level = 8,
                bombChance = 0f,
                random = Random(seed)
            )
        }.map { it.id }.toSet()

        assertTrue(generatedIds.intersect(removedIds).isEmpty())
    }

    @Test
    fun normalPieceLibraryExcludesRemovedPiecesAndMegaBomb() {
        val ids = PieceLibrary.starterPieces.map { it.id }.toSet()

        assertFalse(ids.contains("horizontal_5"))
        assertFalse(ids.contains("vertical_5"))
        assertFalse(ids.contains("square_3x3"))
        assertFalse(ids.contains(JokerType.MegaBomb.toPiece()?.id))
        assertTrue(PieceLibrary.starterPieces.none { it.effect == PieceEffect.MegaBomb })
    }

    @Test
    fun edgeWarningCellsCoverOnlyBoardEdge() {
        val cells = edgeWarningCells()

        assertEquals(28, cells.size)
        assertTrue(cells.contains(Cell(0, 0)))
        assertTrue(cells.contains(Cell(0, Board.SIZE - 1)))
        assertTrue(cells.contains(Cell(Board.SIZE - 1, 0)))
        assertTrue(cells.contains(Cell(Board.SIZE - 1, Board.SIZE - 1)))
        assertTrue(cells.contains(Cell(3, 0)))
        assertTrue(cells.contains(Cell(7, 4)))
        assertFalse(cells.contains(Cell(1, 1)))
        assertFalse(cells.contains(Cell(3, 3)))
    }

    @Test
    fun centerWarningCellsCoverCenterFourByFour() {
        val cells = centerWarningCells()

        assertEquals(16, cells.size)
        assertTrue(cells.contains(Cell(2, 2)))
        assertTrue(cells.contains(Cell(2, 5)))
        assertTrue(cells.contains(Cell(5, 2)))
        assertTrue(cells.contains(Cell(5, 5)))
        assertFalse(cells.contains(Cell(1, 2)))
        assertFalse(cells.contains(Cell(6, 5)))
        assertFalse(cells.contains(Cell(0, 0)))
    }

    @Test
    fun contractWarningCellsOnlyShowForAcceptedForbiddenZoneContracts() {
        val noEdge = testContract(ContractType.NoEdgePlacement)
        val avoidCenter = testContract(ContractType.AvoidCenterArea)
        val lineContract = testContract(ContractType.ClearAtLeastOneLine)

        assertEquals(
            edgeWarningCells(),
            contractWarningCells(ContractState(activeContract = noEdge, isAccepted = true))
        )
        assertEquals(
            centerWarningCells(),
            contractWarningCells(ContractState(activeContract = avoidCenter, isAccepted = true))
        )
        assertTrue(
            contractWarningCells(ContractState(activeContract = lineContract, isAccepted = true)).isEmpty()
        )
        assertTrue(
            contractWarningCells(ContractState(activeContract = noEdge, isAccepted = false)).isEmpty()
        )
    }

    @Test
    fun contractRewardScalesWithLevel() {
        val lowLevelEasyLine = ContractGenerator.generateForType(ContractType.ClearAtLeastOneLine, level = 1)
        val highLevelEasyLine = ContractGenerator.generateForType(ContractType.ClearAtLeastOneLine, level = 4)
        val lowLevelExactLines = ContractGenerator.generateForType(ContractType.ClearExactlyTwoLines, level = 1)
        val highLevelExactLines = ContractGenerator.generateForType(ContractType.ClearExactlyTwoLines, level = 4)

        assertEquals(25, lowLevelEasyLine.rewardPoints)
        assertEquals(40, highLevelEasyLine.rewardPoints)
        assertEquals(40, lowLevelExactLines.rewardPoints)
        assertEquals(55, highLevelExactLines.rewardPoints)
        assertTrue(lowLevelExactLines.rewardPoints > lowLevelEasyLine.rewardPoints)
    }

    @Test
    fun lineClearContractsHaveLowerGenerationWeightThanMediumContracts() {
        val weights = ContractGenerator.contractTypeWeights()
        val lineClearWeight = weights.getValue(ContractType.ClearAtLeastOneLine) +
            weights.getValue(ContractType.ClearExactlyTwoLines)
        val mediumWeight = weights.getValue(ContractType.NoEdgePlacement) +
            weights.getValue(ContractType.AvoidCenterArea) +
            weights.getValue(ContractType.ScoreAtLeastTwenty)

        assertTrue(weights.getValue(ContractType.ClearAtLeastOneLine) < weights.getValue(ContractType.NoEdgePlacement))
        assertTrue(weights.getValue(ContractType.ClearExactlyTwoLines) < weights.getValue(ContractType.AvoidCenterArea))
        assertTrue(lineClearWeight < mediumWeight)
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
        score: Int = 0,
        currentPieces: List<Piece>
    ): GameState {
        return testState(
            board = board,
            score = score,
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
            penaltyPoints = rewardPoints * 2,
            type = type
        )
    }

    private fun placeAllThreeSingles(state: GameState): GameState {
        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 1)
        return GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 2)
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

    private fun expectedSquareCells(
        rowRange: IntRange,
        colRange: IntRange
    ): Set<Cell> {
        return rowRange.flatMap { row ->
            colRange.map { col ->
                Cell(row, col)
            }
        }.toSet()
    }
}

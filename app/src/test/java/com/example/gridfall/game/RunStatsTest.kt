package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunStatsTest {
    @Test
    fun newGameStartsWithZeroRunStats() {
        val state = GameEngine.createInitialState()

        assertEquals(0, state.runStats.linesCleared)
        assertEquals(0, state.runStats.contractsCompleted)
        assertEquals(0, state.runStats.bombsUsed)
        assertEquals(0, state.runStats.megaBombsUsed)
    }

    @Test
    fun lineClearsIncrementRunStatsByRowsAndColumns() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val rowReady = (1 until Board.SIZE).fold(Board.empty()) { board, col ->
            board.fill(0, col)
        }
        val crossReady = (1 until Board.SIZE).fold(rowReady) { board, row ->
            board.fill(row, 0)
        }
        val state = testState(
            board = crossReady,
            currentPieces = listOf(piece)
        )

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)

        assertEquals(2, nextState.runStats.linesCleared)
    }

    @Test
    fun contractCompletionIncrementsRunStats() {
        val piece = Piece("single", listOf(Cell(0, 0)))
        val contract = testContract(ContractType.NoEdgePlacement)
        val state = testState(
            currentPieces = listOf(piece, piece, piece),
            contractState = ContractState(
                activeContract = contract,
                isAccepted = true
            )
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 1, startCol = 1)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 1, startCol = 2)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 1, startCol = 3)

        assertTrue(afterThird.contractState.isCompleted)
        assertEquals(1, afterThird.runStats.contractsCompleted)
    }

    @Test
    fun normalBombUseIncrementsBombsUsed() {
        val state = testState(currentPieces = listOf(bombPiece()))

        val nextState = GameEngine.placePiece(state, pieceIndex = 0, startRow = 2, startCol = 2)

        assertEquals(1, nextState.runStats.bombsUsed)
        assertEquals(0, nextState.runStats.megaBombsUsed)
    }

    @Test
    fun megaBombJokerUseIncrementsMegaBombsUsed() {
        val state = testState(
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.MegaBomb))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.MegaBomb,
            startRow = 2,
            startCol = 2
        )

        assertEquals(0, nextState.runStats.bombsUsed)
        assertEquals(1, nextState.runStats.megaBombsUsed)
    }

    @Test
    fun newGameResetsCurrentRunStatsAndCreatesNewRunId() {
        val firstRun = GameEngine.createInitialState()
        val changedStats = firstRun.runStats
            .recordPlacement(PieceEffect.Bomb, clearedLineCount = 2)
            .recordCompletedContract()

        val restarted = GameEngine.createInitialState()

        assertEquals(2, changedStats.linesCleared)
        assertEquals(1, changedStats.bombsUsed)
        assertEquals(1, changedStats.contractsCompleted)
        assertEquals(0, restarted.runStats.linesCleared)
        assertEquals(0, restarted.runStats.contractsCompleted)
        assertEquals(0, restarted.runStats.bombsUsed)
        assertEquals(0, restarted.runStats.megaBombsUsed)
        assertNotEquals(firstRun.runStats.runId, restarted.runStats.runId)
    }

    @Test
    fun runIdIsStableWhenRunStatsChangeDuringRun() {
        val runStats = RunStats.newRun(nowMillis = 1_000L)

        val changed = runStats
            .recordPlacement(PieceEffect.Bomb, clearedLineCount = 1)
            .recordCompletedContract()

        assertEquals(runStats.runId, changed.runId)
    }

    @Test
    fun durationSecondsUsesElapsedTime() {
        val runStats = RunStats.newRun(nowMillis = 1_000L)

        assertEquals(4, runStats.durationSeconds(nowMillis = 5_900L))
    }

    @Test
    fun revertRestoresPreviousRunStatsAfterJokerMove() {
        val state = testState(
            runStats = RunStats.newRun(nowMillis = 1_000L).copy(
                linesCleared = 2,
                bombsUsed = 1,
                contractsCompleted = 1
            ),
            riskSpinState = RiskSpinState(
                inventory = listOf(JokerType.MegaBomb, JokerType.Revert)
            )
        )
        val afterMegaBomb = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.MegaBomb,
            startRow = 3,
            startCol = 3
        )

        assertEquals(1, afterMegaBomb.runStats.megaBombsUsed)

        val reverted = GameEngine.useRevertJoker(afterMegaBomb)

        assertEquals(state.runStats.runId, reverted.runStats.runId)
        assertEquals(2, reverted.runStats.linesCleared)
        assertEquals(1, reverted.runStats.bombsUsed)
        assertEquals(0, reverted.runStats.megaBombsUsed)
        assertEquals(1, reverted.runStats.contractsCompleted)
        assertNull(reverted.riskSpinState.previousMoveSnapshot)
    }

    @Test
    fun revertDoesNotLeaveInflatedRunStatsOrChain() {
        val state = testState(
            riskSpinState = RiskSpinState(
                inventory = listOf(JokerType.Bomb, JokerType.Revert, JokerType.Revert)
            )
        )
        val afterBomb = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.Bomb,
            startRow = 2,
            startCol = 2
        )
        val reverted = GameEngine.useRevertJoker(afterBomb)
        val secondRevertAttempt = GameEngine.useRevertJoker(reverted)

        assertEquals(0, reverted.runStats.bombsUsed)
        assertEquals(reverted, secondRevertAttempt)
        assertNull(reverted.riskSpinState.previousMoveSnapshot)
    }

    private fun testState(
        board: Board = Board.empty(),
        currentPieces: List<Piece> = listOf(Piece("single", listOf(Cell(0, 0)))),
        usedPieceIndices: Set<Int> = emptySet(),
        score: Int = 0,
        combo: Int = 0,
        isGameOver: Boolean = false,
        contractState: ContractState = ContractState(),
        riskSpinState: RiskSpinState = RiskSpinState(),
        runStats: RunStats = RunStats.newRun(nowMillis = 1_000L)
    ): GameState {
        return GameState(
            board = board,
            currentPieces = currentPieces,
            usedPieceIndices = usedPieceIndices,
            score = score,
            combo = combo,
            isGameOver = isGameOver,
            contractState = contractState,
            riskSpinState = riskSpinState,
            runStats = runStats
        )
    }

    private fun testContract(type: ContractType): Contract {
        return Contract(
            id = type.name,
            title = type.name,
            description = type.name,
            rewardPoints = 25,
            penaltyPoints = 50,
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
}

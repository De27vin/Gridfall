package com.example.gridfall.ui

import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import com.example.gridfall.game.GameState
import com.example.gridfall.game.JokerType
import com.example.gridfall.game.Piece
import com.example.gridfall.game.RiskSpinMemoryField
import com.example.gridfall.game.RiskSpinMemorySession
import com.example.gridfall.game.RiskSpinOption
import com.example.gridfall.game.RiskSpinOutcome
import com.example.gridfall.game.RiskSpinState
import com.example.gridfall.game.RunStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InProgressRunStoreTest {
    @Test
    fun roundTripsInProgressRunIncludingRevertSnapshotAndMemorySession() {
        val previousMove = gameState(score = 5_042, inventory = listOf(JokerType.Revert))
        val currentState = gameState(
            score = 5_073,
            inventory = listOf(JokerType.Bomb, JokerType.Revert),
            previousMove = previousMove
        )
        val session = RiskSpinMemorySession(
            option = RiskSpinOption.Risk,
            revealsLeft = 2,
            fields = listOf(
                RiskSpinMemoryField(
                    id = 0,
                    outcome = RiskSpinOutcome.Bomb,
                    isRevealed = true,
                    jokerAdded = JokerType.Bomb
                ),
                RiskSpinMemoryField(id = 1, outcome = RiskSpinOutcome.Miss)
            )
        )
        val savedRun = SavedInProgressRun(
            gameState = currentState,
            riskSpinMemorySession = session,
            riskSpinPaidCost = 20,
            savedAtEpochMillis = 1234
        )

        assertEquals(savedRun, InProgressRunJson.decode(InProgressRunJson.encode(savedRun)))
    }

    @Test
    fun rejectsMalformedOrGameOverSnapshots() {
        assertNull(InProgressRunJson.decode("not-json"))

        val endedRun = SavedInProgressRun(gameState = gameState().copy(isGameOver = true))
        assertNull(InProgressRunJson.decode(InProgressRunJson.encode(endedRun)))
    }

    @Test
    fun onlyTreatsMeaningfulGameplayAsResumableProgress() {
        val freshRun = GameState(
            board = Board.empty(),
            currentPieces = listOf(piece("fresh")),
            usedPieceIndices = emptySet(),
            score = 0,
            combo = 0,
            isGameOver = false,
            runStats = RunStats(runId = "fresh", startedAtEpochMillis = 1)
        )

        assertFalse(InProgressRunJson.hasProgress(freshRun))
        assertTrue(InProgressRunJson.hasProgress(gameState()))
    }

    private fun gameState(
        score: Int = 5_073,
        inventory: List<JokerType> = listOf(JokerType.Bomb),
        previousMove: GameState? = null
    ): GameState {
        val contract = Contract(
            id = "line-clear",
            title = "Clear a line",
            description = "Clear at least one line.",
            rewardPoints = 25,
            penaltyPoints = 5,
            type = ContractType.ClearAtLeastOneLine
        )
        return GameState(
            board = Board.empty().fill(2, 3, 4),
            currentPieces = listOf(piece("p-1"), piece("p-2")),
            usedPieceIndices = setOf(0),
            score = score,
            combo = 2,
            isGameOver = false,
            contractState = ContractState(
                activeContract = contract,
                isAccepted = true,
                batchPlacedPieces = 1,
                batchClearedLines = 1,
                batchScoreGained = 10,
                completedBatchCount = 3
            ),
            riskSpinState = RiskSpinState(
                inventory = inventory,
                cooldownBatchesRemaining = 4,
                previousMoveSnapshot = previousMove
            ),
            runStats = RunStats(
                runId = "run-$score",
                startedAtEpochMillis = 100,
                linesCleared = 2,
                contractsCompleted = 1,
                bombsUsed = 1
            )
        )
    }

    private fun piece(id: String): Piece {
        return Piece(id = id, cells = listOf(Cell(0, 0)))
    }
}

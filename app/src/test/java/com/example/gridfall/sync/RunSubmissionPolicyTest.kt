package com.example.gridfall.sync

import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.GameState
import com.example.gridfall.game.Piece
import com.example.gridfall.game.RunStats
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSubmissionPolicyTest {
    @Test
    fun registryMarksSameRunSubmittedOnlyOnce() {
        val firstDecision = RunSubmissionRegistry().markSubmittedOnce("run-1")
        val secondDecision = firstDecision.registry.markSubmittedOnce("run-1")

        assertTrue(firstDecision.shouldSubmit)
        assertFalse(secondDecision.shouldSubmit)
    }

    @Test
    fun registryAllowsDifferentRunsToSubmit() {
        val firstDecision = RunSubmissionRegistry().markSubmittedOnce("run-1")
        val secondDecision = firstDecision.registry.markSubmittedOnce("run-2")

        assertTrue(firstDecision.shouldSubmit)
        assertTrue(secondDecision.shouldSubmit)
    }

    @Test
    fun eachNewRunHasUniqueRunId() {
        val firstRun = RunStats.newRun(nowMillis = 1_000L)
        val secondRun = RunStats.newRun(nowMillis = 2_000L)

        assertNotEquals(firstRun.runId, secondRun.runId)
    }

    @Test
    fun gameOverCreatesRunEndingCondition() {
        assertTrue(RunSubmissionPolicy.shouldSubmitGameOverRun(testState(isGameOver = true)))
        assertFalse(RunSubmissionPolicy.shouldSubmitGameOverRun(testState(isGameOver = false)))
    }

    @Test
    fun confirmedRestartWithScoreCreatesRunEndingCondition() {
        assertTrue(RunSubmissionPolicy.shouldSubmitConfirmedRestartRun(testState(score = 10)))
        assertFalse(RunSubmissionPolicy.shouldSubmitConfirmedRestartRun(testState(score = 0)))
    }

    private fun testState(
        score: Int = 0,
        isGameOver: Boolean = false
    ): GameState {
        return GameState(
            board = Board.empty(),
            currentPieces = listOf(Piece("single", listOf(Cell(0, 0)))),
            usedPieceIndices = emptySet(),
            score = score,
            combo = 0,
            isGameOver = isGameOver
        )
    }
}

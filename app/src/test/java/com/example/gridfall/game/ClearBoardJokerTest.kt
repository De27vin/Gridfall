package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearBoardJokerTest {
    @Test
    fun clearBoardJokerClearsPlacedCellsPreservesMapAndAwardsPerfectClear() {
        val blocked = setOf(Cell(0, 0), Cell(2, 3))
        val board = Board.empty(
            rows = 7,
            columns = 10,
            blockedCells = blocked,
            isCustom = true
        ).fill(1, 1, 2).fill(6, 9, 4)
        val state = stateWithClearBoard(board = board, score = 2_000).copy(combo = 4)

        val nextState = GameEngine.useClearBoardJoker(state)

        assertTrue(nextState.board.isEmpty())
        assertEquals(7, nextState.board.rowCount)
        assertEquals(10, nextState.board.columnCount)
        assertEquals(blocked, nextState.board.blockedCells)
        assertTrue(nextState.board.isCustom)
        assertEquals(2_550, nextState.score)
        assertEquals(2_550, nextState.maxScoreReached)
        assertEquals(0, nextState.combo)
        assertTrue(nextState.riskSpinState.inventory.isEmpty())
    }

    @Test
    fun clearBoardJokerDoesNothingOnAnEmptyBoard() {
        val state = stateWithClearBoard(Board.empty(), score = 40)

        val nextState = GameEngine.useClearBoardJoker(state)

        assertSame(state, nextState)
        assertEquals(listOf(JokerType.ClearBoard), nextState.riskSpinState.inventory)
    }

    @Test
    fun clearBoardJokerCanBeRevertedOnce() {
        val board = Board.empty().fill(1, 2, 3).fill(5, 6, 4)
        val state = stateWithClearBoard(board, score = 300).copy(
            riskSpinState = RiskSpinState(
                inventory = listOf(JokerType.ClearBoard, JokerType.Revert)
            )
        )

        val cleared = GameEngine.useClearBoardJoker(state)
        val reverted = GameEngine.useRevertJoker(cleared)

        assertEquals(board, reverted.board)
        assertEquals(300, reverted.score)
        assertEquals(listOf(JokerType.ClearBoard), reverted.riskSpinState.inventory)
        assertEquals(reverted, GameEngine.useRevertJoker(reverted))
    }

    private fun stateWithClearBoard(board: Board, score: Int): GameState {
        return GameEngine.createInitialState().copy(
            board = board,
            score = score,
            maxScoreReached = score,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.ClearBoard))
        )
    }
}

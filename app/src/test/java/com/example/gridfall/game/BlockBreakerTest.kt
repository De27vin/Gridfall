package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockBreakerTest {
    @Test
    fun blockBreakerCannotTargetAnEmptyCellOrConsumeTheItem() {
        val state = stateWithBreaker(Board.empty())

        val nextState = GameEngine.useBlockBreaker(state, row = 2, col = 2)

        assertSame(state, nextState)
        assertEquals(listOf(JokerType.BlockBreaker), nextState.riskSpinState.inventory)
    }

    @Test
    fun blockBreakerRemovesExactlyOneOccupiedCellGivesScoreAndConsumesIt() {
        val board = Board.empty().fill(2, 2, 3).fill(4, 4, 2)
        val state = stateWithBreaker(board, score = 40)

        val nextState = GameEngine.useBlockBreaker(state, row = 2, col = 2)

        assertTrue(nextState.board.isEmpty(2, 2))
        assertEquals(2, nextState.board.get(4, 4))
        assertEquals(45, nextState.score)
        assertEquals(45, nextState.maxScoreReached)
        assertTrue(nextState.riskSpinState.inventory.isEmpty())
    }

    @Test
    fun blockBreakerCannotBeUsedAfterGameOver() {
        val state = stateWithBreaker(Board.empty().fill(1, 1), score = 10).copy(isGameOver = true)

        assertSame(state, GameEngine.useBlockBreaker(state, row = 1, col = 1))
    }

    @Test
    fun riskSpinRewardPoolContainsBlockBreaker() {
        assertTrue(RiskSpinOutcome.BlockBreaker in RiskSpin.rewardPool())
        assertEquals(JokerType.BlockBreaker, RiskSpinOutcome.BlockBreaker.toJokerType())
    }

    @Test
    fun contractBonusAddsBlockBreakerOnlyOnWinningRollAndWhenInventoryHasSpace() {
        val granted = ContractBonusReward.addBlockBreakerIfRolled(emptyList(), rollPercent = 14)
        val missed = ContractBonusReward.addBlockBreakerIfRolled(emptyList(), rollPercent = 15)
        val full = ContractBonusReward.addBlockBreakerIfRolled(
            List(RiskSpinState.MAX_INVENTORY_SIZE) { JokerType.Single },
            rollPercent = 0
        )

        assertEquals(listOf(JokerType.BlockBreaker), granted)
        assertTrue(missed.isEmpty())
        assertEquals(RiskSpinState.MAX_INVENTORY_SIZE, full.size)
        assertFalse(JokerType.BlockBreaker in full)
    }

    private fun stateWithBreaker(board: Board, score: Int = 0): GameState {
        return GameEngine.createInitialState().copy(
            board = board,
            score = score,
            maxScoreReached = score,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.BlockBreaker))
        )
    }
}
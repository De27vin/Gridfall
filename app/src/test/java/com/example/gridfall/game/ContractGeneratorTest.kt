package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ContractGeneratorTest {
    @Test
    fun nextThresholdUsesThirteenToSeventeenHundredPointRange() {
        repeat(50) { seed ->
            val threshold = ContractGenerator.nextContractScoreThreshold(5_000, Random(seed))
            assertTrue(threshold in 6_300..6_700)
        }
    }

    @Test
    fun generatedContractUsesScoreTierRewardAndSixtyFivePercentPenalty() {
        val contract = ContractGenerator.generateForType(
            type = ContractType.ClearExactlyTwoLines,
            score = 8_000
        )

        assertEquals(900, contract.rewardPoints)
        assertEquals(585, contract.penaltyPoints)
    }

    @Test
    fun skipHasNoPenaltyAndCanSafelyOfferOneRareFollowUp() {
        val state = GameState(
            board = Board.empty(),
            currentPieces = listOf(Piece("single", listOf(Cell(0, 0)))),
            usedPieceIndices = emptySet(),
            score = 5_000,
            combo = 0,
            isGameOver = false,
            contractState = ContractState(
                offeredContract = ContractGenerator.generateForType(
                    ContractType.ScoreAtLeastTwenty,
                    score = 5_000
                ),
                nextContractScoreThreshold = 6_300
            )
        )

        val afterSkip = GameEngine.skipContract(state, ZeroRandom)

        assertEquals(5_000, afterSkip.score)
        assertNotNull(afterSkip.contractState.offeredContract)
        assertFalse(afterSkip.contractState.isAccepted)
        assertEquals(6_300, afterSkip.contractState.nextContractScoreThreshold)
    }

    private object ZeroRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}

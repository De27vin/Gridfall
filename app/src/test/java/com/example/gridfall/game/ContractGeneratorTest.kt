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
    fun harderContractUsesRewardMultiplierAndSixtyFivePercentPenalty() {
        val hardContract = ContractGenerator.generateForType(
            type = ContractType.ClearExactlyTwoLines,
            score = 8_000
        )
        val standardContract = ContractGenerator.generateForType(
            type = ContractType.ClearAtLeastOneLine,
            score = 8_000
        )

        assertEquals(900, standardContract.rewardPoints)
        assertEquals(1_125, hardContract.rewardPoints)
        assertEquals(731, hardContract.penaltyPoints)
    }

    @Test
    fun rushPoolAvoidsFullEdgeAndDoubleLineContracts() {
        val types = ContractGenerator.availableTypes(GridLayoutPreset.Rush)

        assertFalse(ContractType.NoEdgePlacement in types)
        assertFalse(ContractType.ClearExactlyTwoLines in types)
        assertFalse(ContractType.ClearTwoLinesInSinglePlacement in types)
        assertTrue(ContractType.AvoidCorners in types)
        assertTrue(ContractType.TouchEdge in types)
    }

    @Test
    fun marathonPoolAvoidsTwoLineRequirements() {
        val types = ContractGenerator.availableTypes(GridLayoutPreset.Marathon)

        assertFalse(ContractType.ClearExactlyTwoLines in types)
        assertFalse(ContractType.ClearTwoLinesInSinglePlacement in types)
        assertTrue(ContractType.TouchEdgeAndCenter in types)
    }

    @Test
    fun scoreTargetsScaleWithGridAndHardLineContractsPayMore() {
        val rushScore = ContractGenerator.generateForType(
            ContractType.ScoreAtLeastTwenty,
            score = 5_000,
            gridLayout = GridLayoutPreset.Rush
        )
        val marathonScore = ContractGenerator.generateForType(
            ContractType.ScoreAtLeastTwenty,
            score = 5_000,
            gridLayout = GridLayoutPreset.Marathon
        )
        val marathonLine = ContractGenerator.generateForType(
            ContractType.ClearAtLeastOneLine,
            score = 5_000,
            gridLayout = GridLayoutPreset.Marathon
        )

        assertEquals(12, rushScore.targetValue)
        assertEquals(30, marathonScore.targetValue)
        assertTrue(marathonLine.rewardPoints > marathonScore.rewardPoints)
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

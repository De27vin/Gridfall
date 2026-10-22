package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun skipHasNoPenaltyAndStartsCooldownInsteadOfOfferingFollowUp() {
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
        assertNull(afterSkip.contractState.offeredContract)
        assertFalse(afterSkip.contractState.isAccepted)
        assertEquals(6_300, afterSkip.contractState.nextContractScoreThreshold)
        assertEquals(8, afterSkip.contractState.batchesUntilNextOffer)
    }

    @Test
    fun highScoreOfferUsesActualScoreRewardAndLongerCooldown() {
        val single = Piece("single", listOf(Cell(0, 0)))
        val state = GameEngine.createInitialState().copy(
            currentPieces = listOf(single, single, single),
            usedPieceIndices = setOf(0, 1),
            score = 25_000,
            contractState = ContractState(nextContractScoreThreshold = 6_300)
        )

        val next = GameEngine.placePiece(state, 2, 1, 1, ZeroRandom)
        val reward = next.contractState.offeredContract!!.rewardPoints

        assertEquals(next.score + reward + 3_000, next.contractState.nextContractScoreThreshold)
        assertEquals(10, next.contractState.batchesUntilNextOffer)
    }

    @Test
    fun highScoreCannotBypassBatchCooldownAndExpiredCooldownStillRequiresPoints() {
        val single = Piece("single", listOf(Cell(0, 0)))
        var state = GameEngine.createInitialState().copy(
            currentPieces = listOf(single, single, single),
            score = 30_000,
            contractState = ContractState(
                nextContractScoreThreshold = 25_000,
                batchesUntilNextOffer = 10
            )
        )

        repeat(9) {
            state = GameEngine.placePiece(
                state.copy(board = Board.empty(), usedPieceIndices = setOf(0, 1)),
                2, 1, 1, ZeroRandom
            )
            assertNull(state.contractState.offeredContract)
        }
        assertEquals(1, state.contractState.batchesUntilNextOffer)

        val belowThreshold = state.copy(
            contractState = state.contractState.copy(nextContractScoreThreshold = 40_000)
        )
        val waitingForPoints = GameEngine.placePiece(
            belowThreshold.copy(board = Board.empty(), usedPieceIndices = setOf(0, 1)),
            2, 1, 1, ZeroRandom
        )
        assertNull(waitingForPoints.contractState.offeredContract)
        assertEquals(0, waitingForPoints.contractState.batchesUntilNextOffer)

        val offered = GameEngine.placePiece(
            state.copy(board = Board.empty(), usedPieceIndices = setOf(0, 1)),
            2, 1, 1, ZeroRandom
        )
        assertTrue(offered.contractState.offeredContract != null)
    }

    @Test
    fun contractSuccessAndImmediateFailurePreserveCooldown() {
        val single = Piece("single", listOf(Cell(0, 0)))
        val state = GameEngine.createInitialState().copy(
            currentPieces = listOf(single, single, single),
            usedPieceIndices = setOf(0, 1),
            score = 25_000,
            contractState = ContractState(
                activeContract = ContractGenerator.generateForType(ContractType.NoEdgePlacement, 25_000),
                isAccepted = true,
                nextContractScoreThreshold = 30_000,
                batchesUntilNextOffer = 10
            )
        )

        val success = GameEngine.placePiece(state, 2, 1, 1, ZeroRandom)
        val failure = GameEngine.placePiece(state, 2, 0, 0, ZeroRandom)

        assertTrue(success.contractState.isCompleted)
        assertTrue(failure.contractState.isFailed)
        assertEquals(10, success.contractState.batchesUntilNextOffer)
        assertEquals(10, failure.contractState.batchesUntilNextOffer)
    }

    private object ZeroRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}

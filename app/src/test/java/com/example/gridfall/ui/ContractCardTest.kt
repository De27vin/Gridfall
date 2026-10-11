package com.example.gridfall.ui

import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractCardTest {
    @Test
    fun reachedScoreTargetShowsCompletionIndicator() {
        val state = acceptedState(
            type = ContractType.ScoreAtLeastTwenty,
            targetValue = 30,
            batchScoreGained = 30
        )

        assertTrue(activeContractGoalReached(state))
    }

    @Test
    fun fullRouteRequiresBothTrackedZones() {
        val edgeOnly = acceptedState(
            type = ContractType.TouchEdgeAndCenter,
            usedEdge = true
        )
        val both = edgeOnly.copy(usedCenter = true)

        assertFalse(activeContractGoalReached(edgeOnly))
        assertTrue(activeContractGoalReached(both))
    }

    @Test
    fun survivalContractsDoNotShowCompletionBeforeBatchResolution() {
        assertFalse(activeContractGoalReached(acceptedState(ContractType.NoEdgePlacement)))
        assertFalse(activeContractGoalReached(acceptedState(ContractType.ClearNoLines)))
    }

    private fun acceptedState(
        type: ContractType,
        targetValue: Int = 0,
        batchScoreGained: Int = 0,
        usedEdge: Boolean = false,
        usedCenter: Boolean = false
    ): ContractState {
        val contract = Contract(
            id = type.name,
            title = type.name,
            description = type.name,
            rewardPoints = 100,
            penaltyPoints = 65,
            type = type,
            targetValue = targetValue
        )
        return ContractState(
            activeContract = contract,
            isAccepted = true,
            batchScoreGained = batchScoreGained,
            usedEdge = usedEdge,
            usedCenter = usedCenter
        )
    }
}

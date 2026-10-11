package com.example.gridfall.ui

import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractGuidanceZonesTest {
    @Test
    fun touchEdgeHighlightsTheOuterLayer() {
        val state = acceptedState(ContractType.TouchEdge)

        assertEquals(edgeWarningCells(7, 7), contractTargetCells(state, 7, 7))
    }

    @Test
    fun touchCenterHighlightsTheCenterArea() {
        val state = acceptedState(ContractType.TouchCenter)

        assertEquals(centerWarningCells(10, 10), contractTargetCells(state, 10, 10))
    }

    @Test
    fun completedSingleZoneStopsHighlightingTheGrid() {
        val edgeDone = acceptedState(ContractType.TouchEdge, usedEdge = true)
        val centerDone = acceptedState(ContractType.TouchCenter, usedCenter = true)

        assertTrue(contractTargetCells(edgeDone, 8, 8).isEmpty())
        assertTrue(contractTargetCells(centerDone, 8, 8).isEmpty())
    }

    @Test
    fun fullRouteOnlyHighlightsTheZonesStillNeeded() {
        val edgeDone = acceptedState(
            type = ContractType.TouchEdgeAndCenter,
            usedEdge = true
        )
        val allDone = edgeDone.copy(usedCenter = true)

        assertEquals(centerWarningCells(8, 10), contractTargetCells(edgeDone, 8, 10))
        assertTrue(contractTargetCells(allDone, 8, 10).isEmpty())
    }

    @Test
    fun unacceptedContractDoesNotHighlightTargets() {
        val state = acceptedState(ContractType.TouchEdge).copy(isAccepted = false)

        assertTrue(contractTargetCells(state, 8, 8).isEmpty())
    }

    private fun acceptedState(
        type: ContractType,
        usedEdge: Boolean = false,
        usedCenter: Boolean = false
    ): ContractState {
        return ContractState(
            activeContract = Contract(
                id = type.name,
                title = type.name,
                description = type.name,
                rewardPoints = 100,
                penaltyPoints = 65,
                type = type
            ),
            isAccepted = true,
            usedEdge = usedEdge,
            usedCenter = usedCenter
        )
    }
}

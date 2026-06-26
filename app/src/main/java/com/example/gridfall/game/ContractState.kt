package com.example.gridfall.game

data class ContractState(
    val offeredContract: Contract? = null,
    val activeContract: Contract? = null,
    val resolvedContract: Contract? = null,
    val isAccepted: Boolean = false,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val batchPlacedPieces: Int = 0,
    val batchClearedLines: Int = 0,
    val batchScoreGained: Int = 0,
    val usedEdge: Boolean = false,
    val usedCenter: Boolean = false,
    val rewardClaimed: Boolean = false,
    val penaltyApplied: Boolean = false,
    val batchesUntilNextOffer: Int = 0,
    val completedBatchCount: Int = 0
)

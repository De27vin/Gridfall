package com.example.gridfall.game

data class GameState(
    val board: Board,
    val currentPieces: List<Piece>,
    val usedPieceIndices: Set<Int>,
    val nextPiece: Piece? = null,
    val placementsInCurrentBatch: Int = 0,
    val score: Int,
    val maxScoreReached: Int = score,
    val combo: Int,
    val isGameOver: Boolean,
    val customBlockPool: List<MapBlockDefinition> = emptyList(),
    val contractState: ContractState = ContractState(),
    val riskSpinState: RiskSpinState = RiskSpinState(),
    val runStats: RunStats = RunStats.newRun()
) {
    val isAwaitingContractDecision: Boolean
        // Offers are not presented after game over; retain restart/undo recovery.
        get() = !isGameOver && contractState.offeredContract != null
}

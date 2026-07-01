package com.example.gridfall.game

data class RiskSpinMemoryField(
    val id: Int,
    val outcome: RiskSpinOutcome,
    val isRevealed: Boolean = false,
    val jokerAdded: JokerType? = null,
    val lostBecauseInventoryFull: Boolean = false
)

data class RiskSpinMemorySession(
    val option: RiskSpinOption,
    val revealsLeft: Int,
    val fields: List<RiskSpinMemoryField>
) {
    val isComplete: Boolean
        get() = revealsLeft <= 0
}

data class RiskSpinMemoryStartResult(
    val state: GameState,
    val session: RiskSpinMemorySession,
    val cost: Int
)

data class RiskSpinMemoryRevealResult(
    val state: GameState,
    val session: RiskSpinMemorySession,
    val entry: RiskSpinResultEntry
)

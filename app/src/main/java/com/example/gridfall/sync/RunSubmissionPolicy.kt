package com.example.gridfall.sync

import com.example.gridfall.game.GameState

data class RunSubmissionRegistry(
    val submittedRunIds: Set<String> = emptySet()
) {
    fun markSubmittedOnce(runId: String): RunSubmissionDecision {
        if (runId in submittedRunIds) {
            return RunSubmissionDecision(
                registry = this,
                shouldSubmit = false
            )
        }

        return RunSubmissionDecision(
            registry = copy(submittedRunIds = submittedRunIds + runId),
            shouldSubmit = true
        )
    }
}

data class RunSubmissionDecision(
    val registry: RunSubmissionRegistry,
    val shouldSubmit: Boolean
)

object RunSubmissionPolicy {
    fun shouldSubmitGameOverRun(state: GameState): Boolean {
        return state.isGameOver
    }

    fun shouldSubmitConfirmedRestartRun(state: GameState): Boolean {
        return state.score > 0
    }
}

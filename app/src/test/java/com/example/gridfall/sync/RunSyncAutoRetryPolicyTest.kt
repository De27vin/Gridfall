package com.example.gridfall.sync

import com.example.gridfall.network.dto.RunSubmissionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class RunSyncAutoRetryPolicyTest {
    @Test
    fun `retry delay backs off and caps at one minute`() {
        assertEquals(5_000L, RunSyncAutoRetryPolicy.delayMillis(0))
        assertEquals(10_000L, RunSyncAutoRetryPolicy.delayMillis(1))
        assertEquals(20_000L, RunSyncAutoRetryPolicy.delayMillis(2))
        assertEquals(40_000L, RunSyncAutoRetryPolicy.delayMillis(3))
        assertEquals(60_000L, RunSyncAutoRetryPolicy.delayMillis(4))
        assertEquals(60_000L, RunSyncAutoRetryPolicy.delayMillis(20))
    }

    @Test
    fun `oldest queued run controls the next retry delay`() {
        val newer = pendingRun("newer", createdAt = 2_000L, attemptCount = 4)
        val older = pendingRun("older", createdAt = 1_000L, attemptCount = 1)

        assertEquals(
            10_000L,
            RunSyncAutoRetryPolicy.delayMillis(listOf(newer, older))
        )
    }

    private fun pendingRun(
        runId: String,
        createdAt: Long,
        attemptCount: Int
    ): PendingRunSubmission {
        return PendingRunSubmission(
            runId = runId,
            request = RunSubmissionRequest(
                score = 100,
                level = 2,
                linesCleared = 3,
                contractsCompleted = 1,
                bombsUsed = 0,
                megaBombsUsed = 0,
                riskSpinsUsed = 0,
                durationSeconds = 60,
                appVersion = "test"
            ),
            createdAtMillis = createdAt,
            attemptCount = attemptCount
        )
    }
}

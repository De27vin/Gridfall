package com.example.gridfall.sync

object RunSyncAutoRetryPolicy {
    private const val FIRST_RETRY_DELAY_MILLIS = 5_000L
    private const val MAX_RETRY_DELAY_MILLIS = 60_000L

    fun delayMillis(attemptCount: Int): Long {
        val exponent = attemptCount.coerceIn(0, 4)
        return (FIRST_RETRY_DELAY_MILLIS shl exponent)
            .coerceAtMost(MAX_RETRY_DELAY_MILLIS)
    }

    fun delayMillis(pendingRuns: List<PendingRunSubmission>): Long {
        val nextRun = pendingRuns.minByOrNull(PendingRunSubmission::createdAtMillis)
        return delayMillis(nextRun?.attemptCount ?: 0)
    }
}

package com.example.gridfall.sync

data class RunSyncRetryResult(
    val syncedCount: Int = 0,
    val retryableFailureCount: Int = 0,
    val permanentFailureCount: Int = 0,
    val skippedBecauseAlreadyRunning: Boolean = false
) {
    val hasFailures: Boolean
        get() = retryableFailureCount > 0 || permanentFailureCount > 0
}

class RunSyncManager(
    private val store: PendingRunQueueStore,
    private val submitPendingRun: suspend (PendingRunSubmission) -> Unit,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private var isRetrying = false

    suspend fun retryPendingRuns(): RunSyncRetryResult {
        if (isRetrying) {
            return RunSyncRetryResult(skippedBecauseAlreadyRunning = true)
        }

        isRetrying = true
        var syncedCount = 0
        var retryableFailureCount = 0
        var permanentFailureCount = 0

        try {
            for (pendingRun in store.load()) {
                val attemptedRun = pendingRun.withAttemptStarted(nowMillis())
                store.savePending(attemptedRun)

                try {
                    submitPendingRun(attemptedRun)
                    store.remove(attemptedRun.runId)
                    syncedCount += 1
                } catch (error: Throwable) {
                    when (RunRetryPolicy.classify(error)) {
                        RunFailureKind.Retryable -> {
                            retryableFailureCount += 1
                            store.savePending(
                                attemptedRun.copy(lastError = RunRetryPolicy.friendlyMessage(error))
                            )
                            break
                        }
                        RunFailureKind.Permanent -> {
                            permanentFailureCount += 1
                            store.remove(attemptedRun.runId)
                        }
                    }
                }
            }
        } finally {
            isRetrying = false
        }

        return RunSyncRetryResult(
            syncedCount = syncedCount,
            retryableFailureCount = retryableFailureCount,
            permanentFailureCount = permanentFailureCount
        )
    }
}

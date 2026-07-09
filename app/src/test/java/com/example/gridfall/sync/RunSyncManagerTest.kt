package com.example.gridfall.sync

import com.example.gridfall.network.HttpStatusException
import com.example.gridfall.network.dto.RunSubmissionRequest
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSyncManagerTest {
    @Test
    fun retryPolicyTreatsNetworkAndServerErrorsAsRetryable() {
        assertEquals(RunFailureKind.Retryable, RunRetryPolicy.classify(IOException("failed to connect")))
        assertEquals(RunFailureKind.Retryable, RunRetryPolicy.classify(HttpStatusException(500, "POST", "/runs")))
        assertEquals(RunFailureKind.Retryable, RunRetryPolicy.classify(HttpStatusException(408, "POST", "/runs")))
        assertEquals(RunFailureKind.Retryable, RunRetryPolicy.classify(HttpStatusException(429, "POST", "/runs")))
    }

    @Test
    fun retryPolicyTreatsPermanentHttpErrorsAsPermanent() {
        assertEquals(RunFailureKind.Permanent, RunRetryPolicy.classify(HttpStatusException(400, "POST", "/runs")))
        assertEquals(RunFailureKind.Permanent, RunRetryPolicy.classify(HttpStatusException(401, "POST", "/runs")))
        assertEquals(RunFailureKind.Permanent, RunRetryPolicy.classify(HttpStatusException(403, "POST", "/runs")))
        assertEquals(RunFailureKind.Permanent, RunRetryPolicy.classify(HttpStatusException(409, "POST", "/runs")))
    }

    @Test
    fun successfulSubmissionRemovesPendingRun() = runBlocking {
        val store = RetryFakePendingRunStore().apply {
            savePending(pendingRun("run-1"))
        }
        val submittedRunIds = mutableListOf<String>()
        val manager = RunSyncManager(
            store = store,
            submitPendingRun = { pendingRun -> submittedRunIds += pendingRun.runId },
            nowMillis = { 2_000L }
        )

        val result = manager.retryPendingRuns()

        assertEquals(listOf("run-1"), submittedRunIds)
        assertEquals(1, result.syncedCount)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun retryableFailureStaysPendingAndIncrementsAttempt() = runBlocking {
        val store = RetryFakePendingRunStore().apply {
            savePending(pendingRun("run-1"))
        }
        val manager = RunSyncManager(
            store = store,
            submitPendingRun = { throw IOException("timeout") },
            nowMillis = { 2_000L }
        )

        val result = manager.retryPendingRuns()
        val pendingRun = store.load().single()

        assertEquals(1, result.retryableFailureCount)
        assertEquals("run-1", pendingRun.runId)
        assertEquals(1, pendingRun.attemptCount)
        assertEquals(2_000L, pendingRun.lastAttemptAtMillis)
        assertEquals("Backend unavailable", pendingRun.lastError)
    }

    @Test
    fun permanentFailureIsRemovedAndDoesNotRetryForever() = runBlocking {
        val store = RetryFakePendingRunStore().apply {
            savePending(pendingRun("run-1"))
        }
        val manager = RunSyncManager(
            store = store,
            submitPendingRun = { throw HttpStatusException(400, "POST", "/runs") },
            nowMillis = { 2_000L }
        )

        val result = manager.retryPendingRuns()

        assertEquals(1, result.permanentFailureCount)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun concurrentRetryCallsDoNotDuplicateSubmissions() = runBlocking {
        val store = RetryFakePendingRunStore().apply {
            savePending(pendingRun("run-1"))
        }
        val submissionStarted = CompletableDeferred<Unit>()
        val releaseSubmission = CompletableDeferred<Unit>()
        var submitCount = 0
        val manager = RunSyncManager(
            store = store,
            submitPendingRun = {
                submitCount += 1
                submissionStarted.complete(Unit)
                releaseSubmission.await()
            },
            nowMillis = { 2_000L }
        )

        val first = async { manager.retryPendingRuns() }
        submissionStarted.await()
        val second = async { manager.retryPendingRuns() }
        releaseSubmission.complete(Unit)

        val results = listOf(first.await(), second.await())

        assertEquals(1, submitCount)
        assertTrue(results.any { it.syncedCount == 1 })
        assertTrue(results.any { it.skippedBecauseAlreadyRunning })
    }

    @Test
    fun failedRunSubmissionQueuesOnlyRetryableFailures() {
        assertTrue(RunRetryPolicy.classify(IOException("failed")) == RunFailureKind.Retryable)
        assertFalse(RunRetryPolicy.classify(HttpStatusException(401, "POST", "/runs")) == RunFailureKind.Retryable)
    }

    private fun pendingRun(runId: String): PendingRunSubmission {
        return PendingRunSubmission(
            runId = runId,
            request = RunSubmissionRequest(
                score = 100,
                level = 2,
                linesCleared = 3,
                contractsCompleted = 1,
                bombsUsed = 0,
                megaBombsUsed = 0,
                durationSeconds = 90,
                appVersion = "1.0"
            ),
            createdAtMillis = 1_000L
        )
    }
}

private class RetryFakePendingRunStore : PendingRunQueueStore {
    private var submissions = emptyList<PendingRunSubmission>()

    override fun load(): List<PendingRunSubmission> = submissions

    override fun savePending(submission: PendingRunSubmission) {
        submissions = PendingRunSubmissionJson.merge(submissions, submission)
    }

    override fun remove(runId: String) {
        submissions = submissions.filterNot { it.runId == runId }
    }

    override fun pendingCount(): Int = submissions.size
}

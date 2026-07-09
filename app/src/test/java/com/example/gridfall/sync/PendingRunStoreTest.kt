package com.example.gridfall.sync

import com.example.gridfall.network.dto.RunSubmissionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingRunStoreTest {
    @Test
    fun startsEmptyWhenJsonIsBlankOrInvalid() {
        assertTrue(PendingRunSubmissionJson.decodeList("").isEmpty())
        assertTrue(PendingRunSubmissionJson.decodeList("not-json").isEmpty())
    }

    @Test
    fun addsPendingRunAndRoundTripsJson() {
        val pendingRun = pendingRun("run-1", score = 150, createdAt = 10)

        val decoded = PendingRunSubmissionJson.decodeList(
            PendingRunSubmissionJson.encodeList(listOf(pendingRun))
        )

        assertEquals(listOf(pendingRun), decoded)
    }

    @Test
    fun doesNotDuplicateSameRunId() {
        val current = listOf(pendingRun("run-1", score = 100, createdAt = 10))
        val updated = PendingRunSubmissionJson.merge(
            current,
            pendingRun("run-1", score = 200, createdAt = 20)
        )

        assertEquals(1, updated.size)
        assertEquals(200, updated.single().request.score)
    }

    @Test
    fun removesRunAfterSuccess() {
        val store = FakePendingRunStore()
        store.savePending(pendingRun("run-1"))
        store.savePending(pendingRun("run-2"))

        store.remove("run-1")

        assertEquals(listOf("run-2"), store.load().map { it.runId })
    }

    @Test
    fun capsQueueAtTwentyAndDropsOldest() {
        val submissions = (1..25).fold(emptyList<PendingRunSubmission>()) { current, index ->
            PendingRunSubmissionJson.merge(
                current,
                pendingRun("run-$index", createdAt = index.toLong())
            )
        }

        assertEquals(20, submissions.size)
        assertEquals("run-6", submissions.first().runId)
        assertEquals("run-25", submissions.last().runId)
    }

    @Test
    fun serializedPendingRunDoesNotStoreSecrets() {
        val json = PendingRunSubmissionJson.encode(pendingRun("run-1"))

        assertFalse(json.contains("token", ignoreCase = true))
        assertFalse(json.contains("password", ignoreCase = true))
        assertFalse(json.contains("secret", ignoreCase = true))
    }

    private fun pendingRun(
        runId: String,
        score: Int = 100,
        createdAt: Long = 1_000L
    ): PendingRunSubmission {
        return PendingRunSubmission(
            runId = runId,
            request = RunSubmissionRequest(
                score = score,
                level = 2,
                linesCleared = 3,
                contractsCompleted = 1,
                bombsUsed = 0,
                megaBombsUsed = 0,
                durationSeconds = 90,
                appVersion = "1.0"
            ),
            createdAtMillis = createdAt,
            attemptCount = 1,
            lastAttemptAtMillis = createdAt + 5,
            lastError = "Backend unavailable"
        )
    }
}

private class FakePendingRunStore : PendingRunQueueStore {
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

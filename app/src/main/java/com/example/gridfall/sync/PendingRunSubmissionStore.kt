package com.example.gridfall.sync

import android.content.Context
import com.example.gridfall.network.dto.RunSubmissionRequest
import org.json.JSONArray
import org.json.JSONObject

data class PendingRunSubmission(
    val runId: String,
    val request: RunSubmissionRequest,
    val createdAtMillis: Long,
    val attemptCount: Int = 0,
    val lastAttemptAtMillis: Long? = null,
    val lastError: String? = null
) {
    fun withAttemptStarted(
        nowMillis: Long,
        error: String? = lastError
    ): PendingRunSubmission {
        return copy(
            attemptCount = attemptCount + 1,
            lastAttemptAtMillis = nowMillis,
            lastError = error
        )
    }
}

interface PendingRunQueueStore {
    fun load(): List<PendingRunSubmission>
    fun savePending(submission: PendingRunSubmission)
    fun remove(runId: String)
    fun pendingCount(): Int
}

class PendingRunSubmissionStore(
    context: Context
) : PendingRunQueueStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): List<PendingRunSubmission> {
        val rawJson = prefs.getString(KEY_PENDING_RUNS, null) ?: return emptyList()
        return PendingRunSubmissionJson.decodeList(rawJson)
    }

    override fun savePending(submission: PendingRunSubmission) {
        saveAll(PendingRunSubmissionJson.merge(load(), submission))
    }

    override fun remove(runId: String) {
        saveAll(load().filterNot { it.runId == runId })
    }

    override fun pendingCount(): Int {
        return load().size
    }

    private fun saveAll(submissions: List<PendingRunSubmission>) {
        prefs.edit()
            .putString(KEY_PENDING_RUNS, PendingRunSubmissionJson.encodeList(submissions))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "gridfall_pending_runs"
        const val KEY_PENDING_RUNS = "pending_runs"
    }
}

object PendingRunSubmissionJson {
    const val MAX_PENDING_RUNS = 20

    fun merge(
        current: List<PendingRunSubmission>,
        submission: PendingRunSubmission
    ): List<PendingRunSubmission> {
        return (current.filterNot { it.runId == submission.runId } + submission)
            .sortedBy { it.createdAtMillis }
            .takeLast(MAX_PENDING_RUNS)
    }

    fun encodeList(submissions: List<PendingRunSubmission>): String {
        val array = JSONArray()
        submissions.forEach { submission ->
            array.put(submission.toJson())
        }
        return array.toString()
    }

    fun decodeList(rawJson: String): List<PendingRunSubmission> {
        return runCatching {
            val array = JSONArray(rawJson)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.toPendingRunSubmission()
            }
        }.getOrDefault(emptyList())
    }

    fun encode(submission: PendingRunSubmission): String {
        return submission.toJson().toString()
    }

    private fun PendingRunSubmission.toJson(): JSONObject {
        return JSONObject()
            .put("runId", runId)
            .put("score", request.score)
            .put("level", request.level)
            .put("linesCleared", request.linesCleared)
            .put("contractsCompleted", request.contractsCompleted)
            .put("bombsUsed", request.bombsUsed)
            .put("megaBombsUsed", request.megaBombsUsed)
            .put("durationSeconds", request.durationSeconds)
            .put("appVersion", request.appVersion)
            .put("createdAt", createdAtMillis)
            .put("attemptCount", attemptCount)
            .put("lastAttemptAt", lastAttemptAtMillis ?: JSONObject.NULL)
            .put("lastError", lastError ?: JSONObject.NULL)
    }

    private fun JSONObject.toPendingRunSubmission(): PendingRunSubmission? {
        val runId = optString("runId").takeIf { it.isNotBlank() } ?: return null

        return PendingRunSubmission(
            runId = runId,
            request = RunSubmissionRequest(
                score = optInt("score"),
                level = optInt("level", 1),
                linesCleared = optInt("linesCleared"),
                contractsCompleted = optInt("contractsCompleted"),
                bombsUsed = optInt("bombsUsed"),
                megaBombsUsed = optInt("megaBombsUsed"),
                durationSeconds = optInt("durationSeconds"),
                appVersion = optString("appVersion").takeIf { it.isNotBlank() } ?: "1.0"
            ),
            createdAtMillis = optLong("createdAt"),
            attemptCount = optInt("attemptCount"),
            lastAttemptAtMillis = optNullableLong("lastAttemptAt"),
            lastError = optString("lastError").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        if (!has(name) || isNull(name)) return null
        return optLong(name)
    }
}

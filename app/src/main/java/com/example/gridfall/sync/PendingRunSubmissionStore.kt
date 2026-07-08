package com.example.gridfall.sync

import android.content.Context
import com.example.gridfall.network.dto.RunSubmissionRequest
import org.json.JSONArray
import org.json.JSONObject

data class PendingRunSubmission(
    val runId: String,
    val request: RunSubmissionRequest
)

class PendingRunSubmissionStore(
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<PendingRunSubmission> {
        val rawJson = prefs.getString(KEY_PENDING_RUNS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawJson)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.toPendingRunSubmission()
            }
        }.getOrDefault(emptyList())
    }

    fun savePending(submission: PendingRunSubmission) {
        val updated = (load().filterNot { it.runId == submission.runId } + submission)
            .takeLast(MAX_PENDING_RUNS)
        saveAll(updated)
    }

    fun remove(runId: String) {
        saveAll(load().filterNot { it.runId == runId })
    }

    fun pendingCount(): Int {
        return load().size
    }

    private fun saveAll(submissions: List<PendingRunSubmission>) {
        val array = JSONArray()
        submissions.forEach { submission ->
            array.put(submission.toJson())
        }
        prefs.edit()
            .putString(KEY_PENDING_RUNS, array.toString())
            .apply()
    }

    private fun PendingRunSubmission.toJson(): JSONObject {
        return JSONObject()
            .put("runId", runId)
            .put("request", request.toJson())
    }

    private fun JSONObject.toPendingRunSubmission(): PendingRunSubmission? {
        val runId = optString("runId").takeIf { it.isNotBlank() } ?: return null
        val requestJson = optJSONObject("request") ?: return null

        return PendingRunSubmission(
            runId = runId,
            request = RunSubmissionRequest(
                score = requestJson.optInt("score"),
                level = requestJson.optInt("level", 1),
                linesCleared = requestJson.optInt("linesCleared"),
                contractsCompleted = requestJson.optInt("contractsCompleted"),
                bombsUsed = requestJson.optInt("bombsUsed"),
                megaBombsUsed = requestJson.optInt("megaBombsUsed"),
                durationSeconds = requestJson.optInt("durationSeconds"),
                appVersion = requestJson.optString("appVersion").takeIf { it.isNotBlank() } ?: "1.0"
            )
        )
    }

    private companion object {
        const val PREFS_NAME = "gridfall_pending_runs"
        const val KEY_PENDING_RUNS = "pending_runs"
        const val MAX_PENDING_RUNS = 10
    }
}

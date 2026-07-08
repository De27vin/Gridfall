package com.example.gridfall.network.dto

import org.json.JSONObject

data class RunSubmissionRequest(
    val score: Int,
    val level: Int,
    val linesCleared: Int,
    val contractsCompleted: Int,
    val bombsUsed: Int,
    val megaBombsUsed: Int,
    val durationSeconds: Int,
    val appVersion: String
) {
    fun toPayloadMap(): Map<String, Any> {
        return mapOf(
            "score" to score,
            "level" to level,
            "linesCleared" to linesCleared,
            "contractsCompleted" to contractsCompleted,
            "bombsUsed" to bombsUsed,
            "megaBombsUsed" to megaBombsUsed,
            "durationSeconds" to durationSeconds,
            "appVersion" to appVersion
        )
    }

    fun toJson(): JSONObject {
        return toPayloadMap().entries.fold(JSONObject()) { json, entry ->
            json.put(entry.key, entry.value)
        }
    }
}

data class RunSubmissionResponse(
    val runId: String?,
    val bestScore: Int?,
    val bestLevel: Int?
)

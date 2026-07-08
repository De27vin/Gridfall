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
    fun toJson(): JSONObject {
        return JSONObject()
            .put("score", score)
            .put("level", level)
            .put("linesCleared", linesCleared)
            .put("contractsCompleted", contractsCompleted)
            .put("bombsUsed", bombsUsed)
            .put("megaBombsUsed", megaBombsUsed)
            .put("durationSeconds", durationSeconds)
            .put("appVersion", appVersion)
    }
}

data class RunSubmissionResponse(
    val runId: String?,
    val bestScore: Int?,
    val bestLevel: Int?
)

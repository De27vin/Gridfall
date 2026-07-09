package com.example.gridfall.sync

import com.example.gridfall.network.HttpStatusException
import java.io.IOException

enum class RunFailureKind {
    Retryable,
    Permanent
}

object RunRetryPolicy {
    fun classify(error: Throwable): RunFailureKind {
        val statusCode = (error as? HttpStatusException)?.statusCode ?: statusCodeFromMessage(error.message)
        if (statusCode != null) {
            return when {
                statusCode == 408 || statusCode == 429 || statusCode >= 500 -> RunFailureKind.Retryable
                statusCode == 400 || statusCode == 401 || statusCode == 403 || statusCode == 409 -> RunFailureKind.Permanent
                else -> RunFailureKind.Permanent
            }
        }

        return if (error is IOException || error.message.orEmpty().contains("timeout", ignoreCase = true)) {
            RunFailureKind.Retryable
        } else {
            RunFailureKind.Permanent
        }
    }

    fun friendlyMessage(error: Throwable): String {
        return when (classify(error)) {
            RunFailureKind.Retryable -> "Backend unavailable"
            RunFailureKind.Permanent -> "Sync failed"
        }
    }

    private fun statusCodeFromMessage(message: String?): Int? {
        val raw = message ?: return null
        return Regex("""HTTP\s+(\d{3})""")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }
}

package com.example.gridfall.sync

enum class RunSyncStatus {
    Idle,
    Submitting,
    Synced,
    Failed
}

data class RunSyncState(
    val runId: String? = null,
    val status: RunSyncStatus = RunSyncStatus.Idle,
    val message: String? = null
)

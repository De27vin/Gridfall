package com.example.gridfall.network

import com.example.gridfall.network.dto.MeResponse

data class AccountConnectionState(
    val isLoading: Boolean = true,
    val firebaseUid: String? = null,
    val isAnonymous: Boolean = true,
    val backendUser: MeResponse? = null,
    val authError: String? = null,
    val backendError: String? = null
) {
    val hasFirebaseUser: Boolean
        get() = firebaseUid != null

    val isBackendConnected: Boolean
        get() = backendUser != null && backendError == null
}


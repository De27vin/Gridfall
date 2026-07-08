package com.example.gridfall.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class GridfallAuthSession(
    val firebaseUid: String,
    val isAnonymous: Boolean,
    val idToken: String
)

class GridfallAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun signInAnonymouslyIfNeeded(): GridfallAuthSession {
        val user = firebaseAuth.currentUser ?: firebaseAuth.signInAnonymously().await().user
            ?: error("Firebase anonymous sign-in returned no user")
        val token = user.getIdToken(false).await().token
            ?: error("Firebase returned no ID token")

        return GridfallAuthSession(
            firebaseUid = user.uid,
            isAnonymous = user.isAnonymous,
            idToken = token
        )
    }
}

private suspend fun <T> Task<T>.await(): T {
    if (isComplete) {
        exception?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(task.result as T)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed")
                )
            }
        }
    }
}


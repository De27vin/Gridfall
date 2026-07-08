package com.example.gridfall.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class GridfallAuthSession(
    val firebaseUid: String,
    val isAnonymous: Boolean,
    val email: String?,
    val idToken: String
)

class GridfallAuthException(message: String) : Exception(message)

class GridfallAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun ensureAnonymousUser(): GridfallAuthSession {
        val user = firebaseAuth.currentUser ?: firebaseAuth.signInAnonymously().await().user
            ?: error("Firebase anonymous sign-in returned no user")
        return sessionForCurrentUser()
    }

    suspend fun signInAnonymouslyIfNeeded(): GridfallAuthSession = ensureAnonymousUser()

    suspend fun registerWithEmailPasswordAndLinkAnonymous(
        email: String,
        password: String
    ): GridfallAuthSession {
        val credential = EmailAuthProvider.getCredential(email.trim(), password)
        try {
            val currentUser = firebaseAuth.currentUser
            val user = if (currentUser != null && currentUser.isAnonymous) {
                currentUser.linkWithCredential(credential).await().user
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await().user
            } ?: error("Firebase registration returned no user")
            return sessionForCurrentUser(user.uid)
        } catch (error: Exception) {
            throw GridfallAuthException(error.friendlyAuthMessage())
        }
    }

    suspend fun loginWithEmailPassword(
        email: String,
        password: String
    ): GridfallAuthSession {
        try {
            val user = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
                ?: error("Firebase login returned no user")
            return sessionForCurrentUser(user.uid)
        } catch (error: Exception) {
            throw GridfallAuthException(error.friendlyAuthMessage())
        }
    }

    suspend fun logoutToAnonymous(): GridfallAuthSession {
        firebaseAuth.signOut()
        return ensureAnonymousUser()
    }

    suspend fun getFreshIdToken(): String {
        val user = firebaseAuth.currentUser ?: throw GridfallAuthException("No Firebase user is signed in.")
        return user.getIdToken(true).await().token ?: throw GridfallAuthException("Firebase returned no ID token.")
    }

    private suspend fun sessionForCurrentUser(expectedUid: String? = null): GridfallAuthSession {
        val user = firebaseAuth.currentUser ?: throw GridfallAuthException("No Firebase user is signed in.")
        if (expectedUid != null && user.uid != expectedUid) {
            throw GridfallAuthException("Firebase account changed unexpectedly.")
        }
        val token = user.getIdToken(false).await().token
            ?: error("Firebase returned no ID token")

        return GridfallAuthSession(
            firebaseUid = user.uid,
            isAnonymous = user.isAnonymous,
            email = user.email,
            idToken = token
        )
    }
}

private fun Exception.friendlyAuthMessage(): String {
    return when (this) {
        is FirebaseAuthUserCollisionException -> "This email already has an account. Please log in instead."
        is FirebaseAuthWeakPasswordException -> "Password is too weak."
        is FirebaseAuthInvalidCredentialsException -> "Please check the email and password."
        is FirebaseAuthInvalidUserException -> "No account was found for this email."
        is GridfallAuthException -> message ?: "Authentication failed."
        else -> message ?: "Authentication failed."
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

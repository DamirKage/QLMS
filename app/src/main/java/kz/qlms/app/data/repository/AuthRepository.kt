package kz.qlms.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.qlms.app.core.QlmsResult

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun register(email: String, password: String): QlmsResult<FirebaseUser> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("No user returned")
    }.fold(
        onSuccess = { QlmsResult.Success(it) },
        onFailure = { QlmsResult.Error(it.message ?: "Registration failed", it) },
    )

    suspend fun login(email: String, password: String): QlmsResult<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user ?: error("No user returned")
    }.fold(
        onSuccess = { QlmsResult.Success(it) },
        onFailure = { QlmsResult.Error(it.message ?: "Login failed", it) },
    )

    suspend fun sendPasswordReset(email: String): QlmsResult<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not send reset email", it) },
    )

    fun signOut() = auth.signOut()

    /** Right to erasure (Kazakhstan's "On Personal Data" law, Art. 21). Firestore doc cleanup happens in UserRepository first. */
    suspend fun deleteAccount(): QlmsResult<Unit> = runCatching {
        auth.currentUser?.delete()?.await() ?: error("No signed-in user")
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not delete account", it) },
    )
}

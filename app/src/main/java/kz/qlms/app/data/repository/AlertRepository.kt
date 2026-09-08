package kz.qlms.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.model.SafetyAlert
import kz.qlms.app.data.remote.FirestoreSchema
import java.util.Date

class AlertRepository(private val firestore: FirebaseFirestore) {

    private fun collection() = firestore.collection(FirestoreSchema.ALERTS)

    /** Dispatcher/admin only — enforced by firestore.rules, not just this call site. */
    suspend fun publish(alert: SafetyAlert): QlmsResult<String> = runCatching {
        val ref = collection().document()
        ref.set(alert.copy(id = ref.id)).await()
        ref.id
    }.fold(
        onSuccess = { QlmsResult.Success(it) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not publish alert", it) },
    )

    /** Alerts created since [sinceEpochMs] — callers filter further by [SafetyAlert.isRelevantTo]. */
    fun observeRecentAlerts(sinceEpochMs: Long): Flow<List<SafetyAlert>> = callbackFlow {
        val registration = collection()
            .whereGreaterThan("createdAt", Date(sinceEpochMs))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val alerts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SafetyAlert::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(alerts)
            }
        awaitClose { registration.remove() }
    }
}

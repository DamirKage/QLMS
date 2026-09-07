package kz.qlms.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.model.EmergencyContact
import kz.qlms.app.data.model.Trip
import kz.qlms.app.data.model.TripStatus
import kz.qlms.app.data.remote.FirestoreSchema

class TripRepository(private val firestore: FirebaseFirestore) {

    private fun collection() = firestore.collection(FirestoreSchema.TRIPS)

    suspend fun startTrip(
        userId: String,
        destination: String,
        expectedArrivalAtEpochMs: Long,
        contacts: List<EmergencyContact>,
        latitude: Double,
        longitude: Double,
    ): QlmsResult<String> = runCatching {
        val docRef = collection().document()
        val trip = Trip(
            id = docRef.id,
            userId = userId,
            destination = destination,
            expectedArrivalAtEpochMs = expectedArrivalAtEpochMs,
            contactsSnapshot = contacts,
            latitude = latitude,
            longitude = longitude,
        )
        docRef.set(trip).await()
        docRef.id
    }.fold(
        onSuccess = { QlmsResult.Success(it) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not start trip", it) },
    )

    fun observeTrip(tripId: String): Flow<Trip?> = callbackFlow {
        val registration = collection().document(tripId).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObject(Trip::class.java)?.copy(id = snapshot.id))
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateLocation(tripId: String, latitude: Double, longitude: Double) {
        runCatching {
            collection().document(tripId).update(mapOf("latitude" to latitude, "longitude" to longitude)).await()
        }
    }

    suspend fun setStatus(tripId: String, status: TripStatus) {
        runCatching { collection().document(tripId).update("statusName", status.name).await() }
    }
}

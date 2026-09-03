package kz.qlms.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kz.qlms.app.core.Constants
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.local.PendingIncidentDao
import kz.qlms.app.data.local.PendingIncidentEntity
import kz.qlms.app.data.local.PendingIncidentPayload
import kz.qlms.app.data.local.toJsonArrayString
import kz.qlms.app.data.local.toJsonString
import kz.qlms.app.data.local.toPendingIncidentPayload
import kz.qlms.app.data.local.toStringListFromJsonArray
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentPrivateDetails
import kz.qlms.app.data.model.IncidentStatus
import kz.qlms.app.data.remote.FirestoreSchema
import kz.qlms.app.data.worker.PendingIncidentSyncWorker
import kz.qlms.app.util.GeoHash
import java.util.UUID
import java.util.concurrent.TimeUnit

class IncidentRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val pendingDao: PendingIncidentDao,
) {
    private fun incidentsCollection() = firestore.collection(FirestoreSchema.INCIDENTS)
    private fun privateDoc(incidentId: String) =
        incidentsCollection().document(incidentId).collection("private").document("dispatch")
    private fun messagesCollection(incidentId: String) =
        incidentsCollection().document(incidentId).collection("messages")

    /**
     * Always queues locally first (Room), then a WorkManager job (see
     * [kz.qlms.app.data.worker.PendingIncidentSyncWorker]) uploads photos and
     * writes to Firestore in the background. This is what makes SOS reliable on
     * a bad connection: the button press succeeds instantly and locally even if
     * the network call behind it hasn't landed yet.
     */
    suspend fun submitIncident(
        incident: Incident,
        privateDetails: IncidentPrivateDetails? = null,
        localPhotoPaths: List<String> = emptyList(),
    ): String {
        val localId = incident.id.ifBlank { UUID.randomUUID().toString() }
        val withGeohash = incident.copy(
            id = localId,
            geohash = GeoHash.encode(incident.latitude, incident.longitude),
            hasPrivateDetails = privateDetails?.isEmpty == false,
        )
        pendingDao.insert(
            PendingIncidentEntity(
                localId = localId,
                incidentJson = PendingIncidentPayload(withGeohash, privateDetails).toJsonString(),
                localPhotoPaths = localPhotoPaths.toJsonArrayString(),
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        enqueueSyncWork()
        return localId
    }

    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<PendingIncidentSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(Constants.PENDING_SYNC_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun observePendingCount(): Flow<Int> = pendingDao.observeCount()

    fun observePendingIncidents(): Flow<List<Incident>> =
        pendingDao.observeAll().map { list -> list.map { it.incidentJson.toPendingIncidentPayload().incident } }

    /** Merges live Firestore incidents (already synced) with the still-queued local ones for a single history list. */
    fun observeMyIncidentsMerged(uid: String): Flow<List<Incident>> =
        observeMyIncidentsRemote(uid).combine(observePendingIncidents()) { remote, pending ->
            val pendingOfMine = pending.filter { it.reporterId == uid }
            pendingOfMine + remote
        }

    private fun observeMyIncidentsRemote(uid: String): Flow<List<Incident>> = callbackFlow {
        val registration = incidentsCollection()
            .whereEqualTo(FirestoreSchema.IncidentFields.REPORTER_ID, uid)
            .orderBy(FirestoreSchema.IncidentFields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, _ ->
                val incidents = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Incident::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(incidents)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Community feed: incidents whose geohash falls in one of the bounding-box
     * prefixes around (lat, lon), re-filtered client-side to the exact radius.
     * Firestore has no native geo-radius query, so this two-step approach (used
     * by most production Firestore geo apps) is the standard workaround. Only
     * the public [Incident] doc is read here — never the private subdocument.
     */
    fun observeNearbyIncidents(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Incident>> = callbackFlow {
        val prefixes = GeoHash.neighborsForRadius(latitude, longitude, radiusKm)
        val registration = incidentsCollection()
            .whereIn(FirestoreSchema.IncidentFields.GEOHASH, prefixes.take(30))
            .orderBy(FirestoreSchema.IncidentFields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, _ ->
                val incidents = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Incident::class.java)?.copy(id = doc.id)
                }?.filter { incident ->
                    incident.status != IncidentStatus.CANCELLED &&
                        GeoHash.distanceKm(latitude, longitude, incident.latitude, incident.longitude) <= radiusKm
                } ?: emptyList()
                trySend(incidents)
            }
        awaitClose { registration.remove() }
    }

    fun observeIncident(incidentId: String): Flow<Incident?> = callbackFlow {
        val registration = incidentsCollection().document(incidentId).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObject(Incident::class.java)?.copy(id = snapshot.id))
        }
        awaitClose { registration.remove() }
    }

    /**
     * Only resolves for the reporter or a dispatcher/admin (enforced by
     * firestore.rules) — anyone else gets a permission-denied, which this
     * turns into a quiet `null` rather than a crash.
     */
    fun observePrivateDetails(incidentId: String): Flow<IncidentPrivateDetails?> = callbackFlow {
        val registration = privateDoc(incidentId).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObject(IncidentPrivateDetails::class.java))
        }
        awaitClose { registration.remove() }
    }

    /**
     * Live back-and-forth with whoever picks up the incident, standing in for
     * a real voice call: the reporter sees "help is on the way" from a human
     * within seconds, without this build depending on a telephony/WebRTC SDK
     * that can't be compile-verified here (see docs/ARCHITECTURE.md).
     */
    fun observeMessages(incidentId: String): Flow<List<kz.qlms.app.data.model.IncidentMessage>> = callbackFlow {
        val registration = messagesCollection(incidentId)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(kz.qlms.app.data.model.IncidentMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(incidentId: String, senderId: String, text: String): QlmsResult<Unit> = runCatching {
        val message = kz.qlms.app.data.model.IncidentMessage(senderId = senderId, text = text)
        messagesCollection(incidentId).document().set(message).await()
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not send message", it) },
    )

    suspend fun cancelIncident(incidentId: String): QlmsResult<Unit> = runCatching {
        incidentsCollection().document(incidentId)
            .update(FirestoreSchema.IncidentFields.STATUS, IncidentStatus.CANCELLED.name)
            .await()
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not cancel", it) },
    )

    /**
     * Fast path used only by the active-SOS service: tries a direct write so the
     * dispatcher panel sees it (and can keep receiving live location updates on
     * the same doc) within seconds. Falls back to the offline queue on any
     * failure — the SOS is never silently lost, it just loses live tracking.
     */
    suspend fun createIncidentDirectOrQueue(incident: Incident, privateDetails: IncidentPrivateDetails?): QlmsResult<String> {
        val hasPrivate = privateDetails?.isEmpty == false
        val withGeohash = incident.copy(
            geohash = GeoHash.encode(incident.latitude, incident.longitude),
            hasPrivateDetails = hasPrivate,
        )
        return runCatching {
            val id = writeToFirestore(withGeohash)
            if (hasPrivate) writePrivateDetails(id, privateDetails!!)
            id
        }.fold(
            onSuccess = { QlmsResult.Success(it) },
            onFailure = {
                submitIncident(withGeohash, privateDetails)
                QlmsResult.Error("Offline — queued for sync", it)
            },
        )
    }

    suspend fun updateIncidentLocation(incidentId: String, latitude: Double, longitude: Double) {
        runCatching {
            incidentsCollection().document(incidentId).update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    FirestoreSchema.IncidentFields.GEOHASH to GeoHash.encode(latitude, longitude),
                ),
            ).await()
        }
    }

    // --- Used by the sync worker only ---

    suspend fun getPendingBatch(): List<PendingIncidentEntity> = pendingDao.getAll()

    suspend fun markSynced(entity: PendingIncidentEntity) = pendingDao.delete(entity)

    suspend fun markAttemptFailed(entity: PendingIncidentEntity, error: String) =
        pendingDao.update(entity.copy(attemptCount = entity.attemptCount + 1, lastError = error))

    suspend fun uploadPhoto(uid: String, localPath: String): String {
        val fileName = "incidents/$uid/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(Uri.parse(localPath)).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun writeToFirestore(incident: Incident): String {
        val docRef = incidentsCollection().document()
        docRef.set(incident.copy(id = docRef.id)).await()
        return docRef.id
    }

    suspend fun writePrivateDetails(incidentId: String, details: IncidentPrivateDetails) {
        privateDoc(incidentId).set(details).await()
    }

    fun payloadOf(entity: PendingIncidentEntity): PendingIncidentPayload = entity.incidentJson.toPendingIncidentPayload()

    fun localPhotoPathsOf(entity: PendingIncidentEntity): List<String> =
        entity.localPhotoPaths.toStringListFromJsonArray()
}

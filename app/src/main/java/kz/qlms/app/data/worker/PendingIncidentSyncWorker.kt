package kz.qlms.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kz.qlms.app.QlmsApplication

/**
 * Drains the offline queue: uploads any local photos, writes the public
 * incident doc (and the private medical/contacts subdocument, if any) to
 * Firestore, then deletes the local copy. Runs with a network constraint (see
 * enqueue site) so WorkManager itself handles "retry once connectivity is
 * back" — this worker only needs to worry about a single attempt.
 */
class PendingIncidentSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as QlmsApplication
        val repository = app.container.incidentRepository
        val pending = repository.getPendingBatch()
        if (pending.isEmpty()) return Result.success()

        var anyFailure = false
        for (entity in pending) {
            try {
                val payload = repository.payloadOf(entity)
                val localPaths = repository.localPhotoPathsOf(entity)
                val uploadedUrls = localPaths.map { path -> repository.uploadPhoto(payload.incident.reporterId, path) }
                val finalIncident = payload.incident.copy(photoUrls = payload.incident.photoUrls + uploadedUrls)
                val id = repository.writeToFirestore(finalIncident)
                if (payload.privateDetails?.isEmpty == false) {
                    repository.writePrivateDetails(id, payload.privateDetails)
                }
                repository.markSynced(entity)
            } catch (t: Throwable) {
                anyFailure = true
                repository.markAttemptFailed(entity, t.message ?: "Unknown sync error")
            }
        }
        return if (anyFailure) Result.retry() else Result.success()
    }
}

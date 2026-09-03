package kz.qlms.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A report/SOS that was created while offline (or while a Firestore write was
 * failing). Non-functional requirement A.2 of the original thesis explicitly
 * asked for offline capability, but the original implementation never provided
 * one — this table plus [PendingIncidentSyncWorker] is what actually delivers it.
 */
@Entity(tableName = "pending_incidents")
data class PendingIncidentEntity(
    @PrimaryKey val localId: String,
    val incidentJson: String,
    val localPhotoPaths: String, // JSON array of local file paths, uploaded on sync
    val createdAtEpochMs: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

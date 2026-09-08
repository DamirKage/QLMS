package kz.qlms.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * The PUBLIC half of a reported event — either a full SOS or a plain community
 * report (crime tip, hazard, etc). This is what the nearby-incidents feed and
 * the map query, so it intentionally excludes anything sensitive: medical info,
 * emergency contacts, and dispatcher notes live in a separate
 * `incidents/{id}/private/dispatch` document (see [IncidentPrivateDetails])
 * that only the reporter and dispatcher/admin accounts can read. Firestore
 * needs a public no-arg constructor and var properties with defaults for
 * automatic (de)serialization via toObject()/data class copy.
 */
data class Incident(
    @get:Exclude @set:Exclude var id: String = "",
    var reporterId: String = "",
    var reporterDisplayName: String = "",
    var isAnonymous: Boolean = false,
    var isSosTriggered: Boolean = false,
    var type: String = IncidentType.OTHER.name,
    var description: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    /** Geohash of (latitude, longitude) at precision 7 — enables cheap "nearby" range queries. */
    var geohash: String = "",
    var photoUrls: List<String> = emptyList(),
    @get:PropertyName("status") @set:PropertyName("status")
    var statusName: String = IncidentStatus.NEW.name,
    var hasPrivateDetails: Boolean = false,
    /**
     * Text-to-911 equivalent: the reporter chose not to (or a trigger path
     * like shake/crash never could) place the 112 call. This lives on the
     * PUBLIC doc, not the private subdoc — a dispatcher must see it the
     * instant the incident loads, before opening any detail, because calling
     * this person back could put them in more danger, not less.
     */
    var isTextOnly: Boolean = false,
    /** Community verification tally — see [IncidentVerification]. Anyone signed in may confirm/dispute; never the reporter's own report. */
    var confirmCount: Int = 0,
    var disputeCount: Int = 0,
    @ServerTimestamp var createdAt: Date? = null,
    @ServerTimestamp var updatedAt: Date? = null,
) {
    @get:Exclude
    val incidentType: IncidentType
        get() = IncidentType.entries.find { it.name == type } ?: IncidentType.OTHER

    @get:Exclude
    val status: IncidentStatus
        get() = IncidentStatus.fromFirestoreValue(statusName)

    /** A report earns a visible "confirmed by the community" badge once enough net confirmations outweigh disputes. */
    @get:Exclude
    val isCommunityVerified: Boolean
        get() = confirmCount - disputeCount >= VERIFIED_NET_THRESHOLD && confirmCount >= VERIFIED_MIN_VOTES

    companion object {
        private const val VERIFIED_NET_THRESHOLD = 3
        private const val VERIFIED_MIN_VOTES = 3
    }
}

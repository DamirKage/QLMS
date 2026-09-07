package kz.qlms.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class TripStatus { ACTIVE, ARRIVED, ALERTED, CANCELLED }

/**
 * "Walk me home" — generalizes the state 112 app's hiking-only route check-in
 * (start point, return-by time, contacts) to any trip, and adds what that
 * feature doesn't have: a live no-login tracking link contacts can watch in
 * real time, not just an after-the-fact alert. Deliberately NOT part of the
 * public incidents feed or feed queries — a trip's live location is only ever
 * visible to the traveler themselves and to whoever holds the specific trip
 * link, never browsable by other citizens.
 */
data class Trip(
    @get:Exclude @set:Exclude var id: String = "",
    var userId: String = "",
    var destination: String = "",
    var statusName: String = TripStatus.ACTIVE.name,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var contactsSnapshot: List<EmergencyContact> = emptyList(),
    @ServerTimestamp var startedAt: Date? = null,
    var expectedArrivalAtEpochMs: Long = 0L,
    var updatedAt: Date? = null,
) {
    @get:Exclude
    val status: TripStatus
        get() = TripStatus.entries.find { it.name == statusName } ?: TripStatus.ACTIVE
}

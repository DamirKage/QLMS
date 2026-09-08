package kz.qlms.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kz.qlms.app.util.GeoHash
import java.util.Date

enum class AlertSeverity { INFO, WARNING, CRITICAL }

/**
 * A dispatcher-issued area broadcast — the reverse-112/Wireless-Emergency-Alerts
 * idea: instead of a citizen calling in, the authorities push a warning out to
 * everyone in an area (a flood, a search for a missing person, a hazard). Fan-out
 * reuses the same geohash-topic infrastructure already built for "incident near
 * you" pushes (see NotificationTopics.kt / functions/index.js) rather than a new
 * mechanism — see docs/ARCHITECTURE.md for why that's a deliberately bounded MVP
 * (whole ~20km topic cells, not a precise circle) rather than the real WEA's
 * carrier-level cell-tower broadcast, which an app can't reach.
 */
data class SafetyAlert(
    @get:Exclude @set:Exclude var id: String = "",
    var title: String = "",
    var body: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var radiusKm: Double = 5.0,
    var severityName: String = AlertSeverity.INFO.name,
    var createdBy: String = "",
    @ServerTimestamp var createdAt: Date? = null,
) {
    @get:Exclude
    val severity: AlertSeverity
        get() = AlertSeverity.entries.find { it.name == severityName } ?: AlertSeverity.INFO

    /**
     * Precise circular check the client applies on top of the server's coarse
     * topic-cell fan-out — a device can be subscribed to a topic cell that
     * overlaps this alert's square footprint without actually being inside
     * its real (circular) radius.
     */
    fun isRelevantTo(userLatitude: Double, userLongitude: Double): Boolean =
        GeoHash.distanceKm(latitude, longitude, userLatitude, userLongitude) <= radiusKm
}

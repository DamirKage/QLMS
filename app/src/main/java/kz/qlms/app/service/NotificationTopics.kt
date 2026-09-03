package kz.qlms.app.service

import com.google.firebase.messaging.FirebaseMessaging
import kz.qlms.app.util.GeoHash

/**
 * FCM topic subscriptions for "incident happened near me" pushes. The client
 * subscribes to a small set of geohash-prefix topics around its last known
 * position (precision 4 ≈ 20km cells — coarse enough that a handful of topics
 * cover a whole city, fine enough that "near me" is actually near). The
 * server-side fan-out that publishes to these topics lives in
 * functions/index.js.
 */
object NotificationTopics {
    private const val PREFIX_PRECISION = 4
    private const val TOPIC_PREFIX = "area_"

    fun subscribeAround(latitude: Double, longitude: Double) {
        val messaging = FirebaseMessaging.getInstance()
        val hash = GeoHash.encode(latitude, longitude, PREFIX_PRECISION)
        messaging.subscribeToTopic("$TOPIC_PREFIX$hash")
    }

    fun unsubscribeFrom(previousLatitude: Double, previousLongitude: Double) {
        val messaging = FirebaseMessaging.getInstance()
        val hash = GeoHash.encode(previousLatitude, previousLongitude, PREFIX_PRECISION)
        messaging.unsubscribeFromTopic("$TOPIC_PREFIX$hash")
    }
}

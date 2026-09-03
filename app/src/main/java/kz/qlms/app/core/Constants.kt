package kz.qlms.app.core

/**
 * Central place for values that show up across layers. Kept as plain constants
 * (no DI framework) so the whole data flow is easy to trace from one file.
 */
object Constants {

    // Kazakhstan moved to a single unified emergency number in 2021; the old
    // per-service numbers still route through it, but SOS should dial the
    // number citizens are actually taught to use today.
    const val EMERGENCY_NUMBER_UNIFIED = "112"
    const val EMERGENCY_NUMBER_POLICE = "102"
    const val EMERGENCY_NUMBER_AMBULANCE = "103"
    const val EMERGENCY_NUMBER_FIRE = "101"
    const val EMERGENCY_NUMBER_GAS = "104"

    const val SOS_COUNTDOWN_SECONDS = 5
    const val SOS_LOCATION_UPDATE_INTERVAL_MS = 5_000L
    const val DEFAULT_NEARBY_RADIUS_KM = 5.0
    const val CHECK_IN_MAX_INTERVAL_MINUTES = 24 * 60L

    const val NOTIFICATION_CHANNEL_SOS = "qlms_sos_channel"
    const val NOTIFICATION_CHANNEL_ALERTS = "qlms_alerts_channel"
    const val NOTIFICATION_CHANNEL_CHECKIN = "qlms_checkin_channel"
    const val NOTIFICATION_CHANNEL_FAKE_CALL = "qlms_fake_call_channel"

    const val FAKE_CALL_DEFAULT_DELAY_SECONDS = 8L

    const val DEEP_LINK_SCHEME = "qlms"

    const val PENDING_SYNC_WORK_NAME = "qlms_pending_incident_sync"
}

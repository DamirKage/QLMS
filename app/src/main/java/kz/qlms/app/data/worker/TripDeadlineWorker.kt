package kz.qlms.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kz.qlms.app.QlmsApplication
import kz.qlms.app.data.model.TripStatus
import kz.qlms.app.util.LocationUtils
import kz.qlms.app.util.PhoneUtils

/**
 * Backup for [kz.qlms.app.service.TripForegroundService]'s own deadline check:
 * scheduled once, at trip start, for the exact expected-arrival instant, so the
 * overdue alert still fires even if the process was killed or the phone died
 * and came back. Checks the trip is still ACTIVE before alerting, so it's a
 * no-op on the (normal) path where the foreground service or an "I arrived"
 * tap already resolved it.
 */
class TripDeadlineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tripId = inputData.getString(KEY_TRIP_ID) ?: return Result.failure()
        val app = applicationContext as QlmsApplication
        val repo = app.container.tripRepository

        val trip = repo.observeTrip(tripId).first() ?: return Result.success()
        if (trip.status != TripStatus.ACTIVE) return Result.success()

        repo.setStatus(tripId, TripStatus.ALERTED)
        val contacts = trip.contactsSnapshot.ifEmpty { app.container.userRepository.getCachedContacts() }
        if (contacts.isNotEmpty()) {
            val location = LocationUtils.getCurrentLocation(applicationContext)
            val link = (location?.let { LocationUtils.googleMapsLink(it.latitude, it.longitude) }
                ?: LocationUtils.googleMapsLink(trip.latitude, trip.longitude))
            val message = applicationContext.getString(kz.qlms.app.R.string.trip_overdue_sms_template, link)
            PhoneUtils.notifyContactsBySms(contacts, message)
        }
        return Result.success()
    }

    companion object {
        const val KEY_TRIP_ID = "trip_id"
    }
}

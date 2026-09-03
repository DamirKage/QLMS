package kz.qlms.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kz.qlms.app.QlmsApplication
import kz.qlms.app.util.LocationUtils
import kz.qlms.app.util.PhoneUtils

/**
 * Periodic "Safety Check-in" watchdog: if the user armed it and hasn't tapped
 * "I'm safe" within their chosen window, this notifies their emergency
 * contacts once (not on every run) with the last known location.
 */
class CheckInWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as QlmsApplication
        val settings = app.container.settingsDataStore.settingsFlow.first()
        if (!settings.checkInEnabled) return Result.success()

        val lastCheckIn = app.container.settingsDataStore.lastCheckInFlow.first()
        val overdueMs = System.currentTimeMillis() - lastCheckIn
        val windowMs = settings.checkInIntervalMinutes * 60_000L
        if (overdueMs < windowMs) return Result.success()

        if (app.container.settingsDataStore.wasCheckInAlertSent()) return Result.success()

        val contacts = app.container.userRepository.getCachedContacts()
        if (contacts.isEmpty()) return Result.success()

        val location = LocationUtils.getCurrentLocation(applicationContext)
        val link = location?.let { LocationUtils.googleMapsLink(it.latitude, it.longitude) }.orEmpty()
        val message = applicationContext.getString(kz.qlms.app.R.string.checkin_sms_template, link)
        PhoneUtils.notifyContactsBySms(contacts, message)
        app.container.settingsDataStore.markCheckInAlertSent()

        return Result.success()
    }
}

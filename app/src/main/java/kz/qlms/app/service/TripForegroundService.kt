package kz.qlms.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qlms.app.MainActivity
import kz.qlms.app.QlmsApplication
import kz.qlms.app.R
import kz.qlms.app.core.Constants
import kz.qlms.app.data.model.TripStatus
import kz.qlms.app.util.LocationUtils
import kz.qlms.app.util.PhoneUtils

/**
 * Runs for the lifetime of an active Trip ("walk me home"): pushes fresh
 * location every ~20s so the share link stays live, and — if the app process
 * survives that long — fires the "didn't arrive" alert itself the moment the
 * deadline passes. [kz.qlms.app.data.worker.TripDeadlineWorker] is the backup
 * for when it doesn't (app killed, phone died): it's scheduled once, from the
 * same start call, to check the same condition from outside this service.
 */
class TripForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tripId = intent?.getStringExtra(EXTRA_TRIP_ID) ?: return START_NOT_STICKY
        val destination = intent.getStringExtra(EXTRA_DESTINATION).orEmpty()
        val deadline = intent.getLongExtra(EXTRA_DEADLINE_EPOCH_MS, 0L)

        startForeground(NOTIFICATION_ID, buildNotification(destination))
        _activeTripId.value = tripId

        scope.launch { runTripFlow(tripId, deadline) }
        return START_STICKY
    }

    private suspend fun runTripFlow(tripId: String, deadlineEpochMs: Long) {
        val app = applicationContext as QlmsApplication
        val repo = app.container.tripRepository

        while (scope.isActiveJob()) {
            val now = System.currentTimeMillis()
            if (now >= deadlineEpochMs) {
                fireOverdueAlert(tripId)
                break
            }
            val location = LocationUtils.getCurrentLocation(applicationContext)
            if (location != null) repo.updateLocation(tripId, location.latitude, location.longitude)
            delay(Constants.TRIP_LOCATION_UPDATE_INTERVAL_MS)
        }
        stopSelf()
    }

    private suspend fun fireOverdueAlert(tripId: String) {
        val app = applicationContext as QlmsApplication
        val repo = app.container.tripRepository
        val userRepo = app.container.userRepository
        repo.setStatus(tripId, TripStatus.ALERTED)
        val location = LocationUtils.getCurrentLocation(applicationContext)
        val contacts = userRepo.getCachedContacts()
        if (contacts.isNotEmpty()) {
            val link = location?.let { LocationUtils.googleMapsLink(it.latitude, it.longitude) }.orEmpty()
            PhoneUtils.notifyContactsBySms(contacts, getString(R.string.trip_overdue_sms_template, link))
        }
    }

    private fun CoroutineScope.isActiveJob() = coroutineContext[Job]?.isActive == true

    override fun onDestroy() {
        _activeTripId.value?.let { TripScheduler.cancel(applicationContext, it) }
        _activeTripId.value = null
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(destination: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val arrivedIntent = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, TripActionReceiver::class.java).setAction(TripActionReceiver.ACTION_ARRIVED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_CHECKIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.trip_notification_title))
            .setContentText(getString(R.string.trip_notification_text, destination))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.trip_arrived_action), arrivedIntent)
            .build()
    }

    companion object {
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_DESTINATION = "extra_destination"
        const val EXTRA_DEADLINE_EPOCH_MS = "extra_deadline_epoch_ms"
        private const val NOTIFICATION_ID = 4401

        private val _activeTripId = MutableStateFlow<String?>(null)
        val activeTripId = _activeTripId.asStateFlow()

        fun start(context: android.content.Context, tripId: String, destination: String, deadlineEpochMs: Long) {
            val intent = Intent(context, TripForegroundService::class.java)
                .putExtra(EXTRA_TRIP_ID, tripId)
                .putExtra(EXTRA_DESTINATION, destination)
                .putExtra(EXTRA_DEADLINE_EPOCH_MS, deadlineEpochMs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
            TripScheduler.scheduleDeadlineAlert(context, tripId, deadlineEpochMs)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, TripForegroundService::class.java))
        }
    }
}

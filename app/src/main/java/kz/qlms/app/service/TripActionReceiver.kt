package kz.qlms.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kz.qlms.app.QlmsApplication
import kz.qlms.app.data.model.TripStatus

/** Handles the "I arrived" action tapped from the persistent trip notification. */
class TripActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ARRIVED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as QlmsApplication
        val tripId = TripForegroundService.activeTripId.value

        CoroutineScope(Dispatchers.IO).launch {
            if (tripId != null) {
                app.container.tripRepository.setStatus(tripId, TripStatus.ARRIVED)
                TripScheduler.cancel(context, tripId)
            }
            context.stopService(Intent(context, TripForegroundService::class.java))
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_ARRIVED = "kz.qlms.app.action.TRIP_ARRIVED"
    }
}

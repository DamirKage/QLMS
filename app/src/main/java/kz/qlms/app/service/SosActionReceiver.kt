package kz.qlms.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kz.qlms.app.QlmsApplication

/** Handles the "Cancel" action tapped from the persistent SOS notification. */
class SosActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_SOS) return

        val pendingResult = goAsync()
        val app = context.applicationContext as QlmsApplication
        val incidentId = SosForegroundService.currentIncidentId.value

        CoroutineScope(Dispatchers.IO).launch {
            if (incidentId != null) {
                app.container.incidentRepository.cancelIncident(incidentId)
            }
            context.stopService(Intent(context, SosForegroundService::class.java))
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_CANCEL_SOS = "kz.qlms.app.action.CANCEL_SOS"
    }
}

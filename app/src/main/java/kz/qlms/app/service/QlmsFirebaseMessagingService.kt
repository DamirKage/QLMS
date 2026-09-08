package kz.qlms.app.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kz.qlms.app.MainActivity
import kz.qlms.app.R
import kz.qlms.app.core.Constants

/**
 * Receives: (a) "new incident near you" pushes fanned out server-side by geohash
 * topic (see functions/index.js for the reference Cloud Function), (b)
 * status-change pushes when a dispatcher updates one of the user's own
 * reports, and (c) dispatcher-issued area safety alerts (data["type"] ==
 * "area_alert" — see AlertRepository/onAlertCreated), shown on their own
 * higher-importance channel since they're meant to be WEA-style attention
 * grabbing rather than a routine heads-up. Token refresh is a no-op here —
 * nothing server-side keys off a per-device token in this design, only topic
 * subscriptions (see [NotificationTopics]), so there's nothing to persist.
 */
class QlmsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        val deepLinkIncidentId = message.data["incidentId"]
        val isAreaAlert = message.data["type"] == "area_alert"

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                deepLinkIncidentId?.let { putExtra(MainActivity.EXTRA_DEEP_LINK_INCIDENT_ID, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channel = if (isAreaAlert) Constants.NOTIFICATION_CHANNEL_AREA_ALERTS else Constants.NOTIFICATION_CHANNEL_ALERTS
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(if (isAreaAlert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}

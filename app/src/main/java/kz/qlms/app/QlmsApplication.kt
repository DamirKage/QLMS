package kz.qlms.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import kz.qlms.app.core.AppContainer
import kz.qlms.app.core.Constants

class QlmsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_SOS,
                getString(R.string.notification_channel_sos_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_sos_desc)
                enableLights(true)
                enableVibration(true)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ALERTS,
                getString(R.string.notification_channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_alerts_desc)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_CHECKIN,
                getString(R.string.notification_channel_checkin_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_checkin_desc)
            },
        )
    }
}

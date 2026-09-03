package kz.qlms.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kz.qlms.app.R
import kz.qlms.app.core.Constants

/**
 * Fires after the scheduling delay and posts a full-screen-intent
 * notification — the same mechanism real dialer apps use for an incoming
 * call, so it launches [FakeCallActivity] over the lock screen even if the
 * app was backgrounded in the meantime.
 */
class FakeCallWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val callerName = inputData.getString(KEY_CALLER_NAME)

        val fullScreenIntent = Intent(context, FakeCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FakeCallActivity.EXTRA_CALLER_NAME, callerName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_FAKE_CALL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(callerName ?: context.getString(R.string.fake_call_default_name))
            .setContentText(context.getString(R.string.fake_call_incoming_label))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        const val KEY_CALLER_NAME = "caller_name"
        private const val NOTIFICATION_ID = 4301
    }
}

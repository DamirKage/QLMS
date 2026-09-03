package kz.qlms.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kz.qlms.app.core.Constants
import kz.qlms.app.data.worker.PendingIncidentSyncWorker
import java.util.concurrent.TimeUnit

/** Re-arms the background sync of anything still stuck in the offline queue after a reboot. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val request = PeriodicWorkRequestBuilder<PendingIncidentSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "${Constants.PENDING_SYNC_WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

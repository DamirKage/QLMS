package kz.qlms.app.service

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kz.qlms.app.data.worker.TripDeadlineWorker
import java.util.concurrent.TimeUnit

/** Schedules/cancels the [TripDeadlineWorker] backup alert for a trip's deadline. */
object TripScheduler {

    fun scheduleDeadlineAlert(context: Context, tripId: String, deadlineEpochMs: Long) {
        val delayMs = (deadlineEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<TripDeadlineWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(TripDeadlineWorker.KEY_TRIP_ID, tripId).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(tripId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, tripId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(tripId))
    }

    private fun workName(tripId: String) = "qlms_trip_deadline_$tripId"
}

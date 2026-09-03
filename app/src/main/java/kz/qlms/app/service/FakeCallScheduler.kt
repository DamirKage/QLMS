package kz.qlms.app.service

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kz.qlms.app.core.Constants
import java.util.concurrent.TimeUnit

object FakeCallScheduler {
    fun schedule(context: Context, callerName: String, delaySeconds: Long = Constants.FAKE_CALL_DEFAULT_DELAY_SECONDS) {
        val request = OneTimeWorkRequestBuilder<FakeCallWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(Data.Builder().putString(FakeCallWorker.KEY_CALLER_NAME, callerName).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

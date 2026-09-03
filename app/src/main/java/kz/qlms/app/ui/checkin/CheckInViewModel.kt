package kz.qlms.app.ui.checkin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qlms.app.data.local.SettingsDataStore
import kz.qlms.app.data.model.AppSettings
import kz.qlms.app.data.worker.CheckInWorker
import java.util.concurrent.TimeUnit

class CheckInViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val appContext: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val lastCheckIn: StateFlow<Long> = settingsDataStore.lastCheckInFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    fun setEnabled(enabled: Boolean, intervalMinutes: Long) {
        viewModelScope.launch {
            settingsDataStore.setCheckIn(enabled, intervalMinutes)
            settingsDataStore.recordCheckIn()
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<CheckInWorker>(15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(appContext)
                    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else {
                WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
            }
        }
    }

    fun checkInNow() {
        viewModelScope.launch { settingsDataStore.recordCheckIn() }
    }

    companion object {
        private const val WORK_NAME = "qlms_checkin_watchdog"
    }
}

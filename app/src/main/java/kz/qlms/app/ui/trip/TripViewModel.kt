package kz.qlms.app.ui.trip

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.model.Trip
import kz.qlms.app.data.model.TripStatus
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.TripRepository
import kz.qlms.app.data.repository.UserRepository
import kz.qlms.app.service.TripForegroundService
import kz.qlms.app.service.TripScheduler
import kz.qlms.app.util.LocationUtils

class TripViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appContext: Context,
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeTrip: StateFlow<Trip?> = TripForegroundService.activeTripId
        .flatMapLatest { id -> if (id == null) flowOf(null) else tripRepository.observeTrip(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var isStarting by mutableStateOf(false)
        private set

    fun startTrip(destination: String, durationMinutes: Long) {
        val uid = authRepository.currentUser?.uid ?: return
        if (destination.isBlank() || isStarting) return
        viewModelScope.launch {
            isStarting = true
            val location = LocationUtils.getCurrentLocation(appContext)
            val contacts = userRepository.getCachedContacts()
            val deadline = System.currentTimeMillis() + durationMinutes * 60_000L
            val result = tripRepository.startTrip(
                userId = uid,
                destination = destination,
                expectedArrivalAtEpochMs = deadline,
                contacts = contacts,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
            )
            isStarting = false
            if (result is QlmsResult.Success) {
                TripForegroundService.start(appContext, result.data, destination, deadline)
            }
        }
    }

    fun markArrived(tripId: String) {
        viewModelScope.launch {
            tripRepository.setStatus(tripId, TripStatus.ARRIVED)
            TripScheduler.cancel(appContext, tripId)
            TripForegroundService.stop(appContext)
        }
    }

    fun cancelTrip(tripId: String) {
        viewModelScope.launch {
            tripRepository.setStatus(tripId, TripStatus.CANCELLED)
            TripScheduler.cancel(appContext, tripId)
            TripForegroundService.stop(appContext)
        }
    }
}

package kz.qlms.app.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qlms.app.data.model.AppSettings
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.IncidentRepository
import kz.qlms.app.service.NotificationTopics

class HomeViewModel(
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository,
    settingsFlow: kotlinx.coroutines.flow.Flow<AppSettings>,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    val pendingCount: StateFlow<Int> = incidentRepository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val radiusKm = settingsFlow.flatMapLatest { flowOfSingle(it.nearbyRadiusKm) }

    val nearbyIncidents: StateFlow<List<Incident>> = _userLocation
        .flatMapLatest { location ->
            if (location == null) {
                flowOfSingle(emptyList())
            } else {
                incidentRepository.observeNearbyIncidents(location.latitude, location.longitude, 5.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onLocationResolved(location: Location) {
        _userLocation.value = location
        viewModelScope.launch {
            NotificationTopics.subscribeAround(location.latitude, location.longitude)
        }
    }

    private fun <T> flowOfSingle(value: T) = kotlinx.coroutines.flow.flowOf(value)
}

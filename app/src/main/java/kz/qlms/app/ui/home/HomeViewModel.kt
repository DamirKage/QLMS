package kz.qlms.app.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qlms.app.data.model.AppSettings
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.data.model.SafetyAlert
import kz.qlms.app.data.repository.AlertRepository
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.IncidentRepository
import kz.qlms.app.service.NotificationTopics

private const val ALERTS_LOOKBACK_MS = 24 * 60 * 60 * 1000L

class HomeViewModel(
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository,
    private val alertRepository: AlertRepository,
    settingsFlow: kotlinx.coroutines.flow.Flow<AppSettings>,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    val pendingCount: StateFlow<Int> = incidentRepository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val recentAlerts: StateFlow<List<SafetyAlert>> = alertRepository
        .observeRecentAlerts(System.currentTimeMillis() - ALERTS_LOOKBACK_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Server-side fan-out is a coarse ~20km-cell match; this is the precise circular check on top of it. */
    val relevantAlerts: StateFlow<List<SafetyAlert>> = combine(recentAlerts, _userLocation) { alerts, location ->
        if (location == null) emptyList() else alerts.filter { it.isRelevantTo(location.latitude, location.longitude) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

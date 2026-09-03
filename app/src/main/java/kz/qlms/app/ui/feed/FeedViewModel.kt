package kz.qlms.app.ui.feed

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kz.qlms.app.core.Constants
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.repository.IncidentRepository

class FeedViewModel(private val incidentRepository: IncidentRepository) : ViewModel() {

    private val _location = MutableStateFlow<Location?>(null)

    val nearbyIncidents: StateFlow<List<Incident>> = _location
        .flatMapLatest { location ->
            if (location == null) {
                flowOf(emptyList())
            } else {
                incidentRepository.observeNearbyIncidents(location.latitude, location.longitude, Constants.DEFAULT_NEARBY_RADIUS_KM)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLocation(location: Location) {
        _location.value = location
    }
}

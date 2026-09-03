package kz.qlms.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentPrivateDetails
import kz.qlms.app.data.repository.IncidentRepository

class IncidentDetailViewModel(
    private val incidentRepository: IncidentRepository,
    incidentId: String,
) : ViewModel() {

    val incident: StateFlow<Incident?> = incidentRepository.observeIncident(incidentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Resolves to null for anyone but the reporter or a dispatcher — see firestore.rules. */
    val privateDetails: StateFlow<IncidentPrivateDetails?> = incidentRepository.observePrivateDetails(incidentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun cancel(incidentId: String) {
        viewModelScope.launch { incidentRepository.cancelIncident(incidentId) }
    }
}

package kz.qlms.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.IncidentRepository

class HistoryViewModel(
    incidentRepository: IncidentRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val myIncidents: StateFlow<List<Incident>> = authRepository.authStateFlow()
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else incidentRepository.observeMyIncidentsMerged(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

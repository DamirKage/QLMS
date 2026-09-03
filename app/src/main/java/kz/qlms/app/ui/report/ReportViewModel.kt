package kz.qlms.app.ui.report

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.IncidentRepository

sealed class ReportUiState {
    data object Idle : ReportUiState()
    data object Submitting : ReportUiState()
    data object Submitted : ReportUiState()
}

class ReportViewModel(
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun submit(
        type: IncidentType,
        description: String,
        address: String,
        isAnonymous: Boolean,
        location: Location?,
        localPhotoPaths: List<String>,
    ) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = ReportUiState.Submitting
            val incident = Incident(
                reporterId = uid,
                isAnonymous = isAnonymous,
                type = type.name,
                description = description,
                address = address,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
            )
            incidentRepository.submitIncident(incident, localPhotoPaths = localPhotoPaths)
            _uiState.value = ReportUiState.Submitted
        }
    }
}

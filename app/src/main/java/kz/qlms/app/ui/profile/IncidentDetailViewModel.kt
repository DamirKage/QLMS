package kz.qlms.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentMessage
import kz.qlms.app.data.model.IncidentPrivateDetails
import kz.qlms.app.data.model.VerificationVote
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.IncidentRepository

class IncidentDetailViewModel(
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository,
    private val incidentId: String,
) : ViewModel() {

    val incident: StateFlow<Incident?> = incidentRepository.observeIncident(incidentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Resolves to null for anyone but the reporter or a dispatcher — see firestore.rules. */
    val privateDetails: StateFlow<IncidentPrivateDetails?> = incidentRepository.observePrivateDetails(incidentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Live chat with the dispatcher — the practical, reliable stand-in for a voice call (see IncidentMessage). */
    val messages: StateFlow<List<IncidentMessage>> = incidentRepository.observeMessages(incidentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUid: String? get() = authRepository.currentUser?.uid

    val myVerificationVote: StateFlow<VerificationVote?> =
        (currentUid?.let { incidentRepository.observeMyVerificationVote(incidentId, it) } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun cancel(incidentId: String) {
        viewModelScope.launch { incidentRepository.cancelIncident(incidentId) }
    }

    fun castVerificationVote(vote: VerificationVote) {
        val uid = currentUid ?: return
        viewModelScope.launch { incidentRepository.castVerificationVote(incidentId, uid, vote) }
    }

    fun sendMessage(text: String) {
        val uid = authRepository.currentUser?.uid ?: return
        if (text.isBlank()) return
        viewModelScope.launch { incidentRepository.sendMessage(incidentId, uid, text.trim()) }
    }
}

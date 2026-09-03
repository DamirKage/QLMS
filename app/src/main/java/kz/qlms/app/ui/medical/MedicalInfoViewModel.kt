package kz.qlms.app.ui.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qlms.app.data.model.MedicalProfile
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.UserRepository

class MedicalInfoViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow(userRepository.getCachedMedicalProfile())
    val profile: StateFlow<MedicalProfile> = _profile.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(profile: MedicalProfile) {
        val uid = authRepository.currentUser?.uid ?: return
        _profile.value = profile
        viewModelScope.launch {
            userRepository.saveMedicalProfile(uid, profile)
            _saved.value = true
        }
    }

    fun consumeSaved() {
        _saved.value = false
    }
}

package kz.qlms.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.model.UserProfile
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.UserRepository

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                when (val result = userRepository.getProfile(uid)) {
                    is QlmsResult.Success -> _profile.value = result.data
                    else -> Unit
                }
            }
        }
    }

    fun signOut() = authRepository.signOut()
}

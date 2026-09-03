package kz.qlms.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.UserRepository

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _resetEmailSent = MutableStateFlow(false)
    val resetEmailSent: StateFlow<Boolean> = _resetEmailSent.asStateFlow()

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("fields_required")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("password_mismatch")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("password_too_short")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.register(email.trim(), password)) {
                is QlmsResult.Success -> {
                    userRepository.createProfileIfMissing(result.data.uid, fullName.trim(), email.trim())
                    _uiState.value = AuthUiState.Success
                }
                is QlmsResult.Error -> _uiState.value = AuthUiState.Error(result.message)
                QlmsResult.Loading -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("fields_required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.login(email.trim(), password)) {
                is QlmsResult.Success -> _uiState.value = AuthUiState.Success
                is QlmsResult.Error -> _uiState.value = AuthUiState.Error(result.message)
                QlmsResult.Loading -> Unit
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("fields_required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.sendPasswordReset(email.trim())) {
                is QlmsResult.Success -> {
                    _resetEmailSent.value = true
                    _uiState.value = AuthUiState.Idle
                }
                is QlmsResult.Error -> _uiState.value = AuthUiState.Error(result.message)
                QlmsResult.Loading -> Unit
            }
        }
    }

    fun consumeError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }
}

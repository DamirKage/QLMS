package kz.qlms.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qlms.app.data.local.SettingsDataStore
import kz.qlms.app.data.model.AppLanguage
import kz.qlms.app.data.model.AppSettings
import kz.qlms.app.data.model.ThemeMode
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.UserRepository

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settingsDataStore.setLanguage(language) }
    fun setRadiusKm(radiusKm: Double) = viewModelScope.launch { settingsDataStore.setNearbyRadiusKm(radiusKm) }
    fun setHighContrast(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setHighContrastText(enabled) }
    fun setSilentSos(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setSilentSosEnabled(enabled) }

    fun deleteAccountAndData() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.deleteAllUserData(uid)
            authRepository.deleteAccount()
            _accountDeleted.value = true
        }
    }
}

package kz.qlms.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qlms.app.data.model.EmergencyContact
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.UserRepository

class EmergencyContactsViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _contacts = MutableStateFlow(userRepository.getCachedContacts())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    fun addOrUpdate(contact: EmergencyContact) {
        val updated = _contacts.value.filterNot { it.id == contact.id } + contact
        persist(updated)
    }

    fun remove(contactId: String) {
        persist(_contacts.value.filterNot { it.id == contactId })
    }

    private fun persist(list: List<EmergencyContact>) {
        _contacts.value = list
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch { userRepository.saveContacts(uid, list) }
    }
}

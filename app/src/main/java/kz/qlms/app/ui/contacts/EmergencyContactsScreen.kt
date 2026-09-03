package kz.qlms.app.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.data.model.EmergencyContact
import kz.qlms.app.ui.components.EmptyState
import kz.qlms.app.ui.components.QlmsPrimaryButton
import kz.qlms.app.ui.components.QlmsTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: EmergencyContactsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EmergencyContactsViewModel(container.userRepository, container.authRepository) }
        },
    )
    val contacts by viewModel.contacts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.contacts_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.contacts_add))
            }
        },
    ) { padding ->
        if (contacts.isEmpty()) {
            EmptyState(text = stringResource(R.string.contacts_empty), modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    ContactRow(
                        contact = contact,
                        onToggleNotify = { viewModel.addOrUpdate(contact.copy(notifyOnSos = it)) },
                        onDelete = { viewModel.remove(contact.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onSave = { contact ->
                viewModel.addOrUpdate(contact)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ContactRow(contact: EmergencyContact, onToggleNotify: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(contact.relationship, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
            }
            Checkbox(checked = contact.notifyOnSos, onCheckedChange = onToggleNotify)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (EmergencyContact) -> Unit) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contacts_add)) },
        text = {
            Column {
                QlmsTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.field_full_name))
                Spacer(Modifier.height(8.dp))
                QlmsTextField(value = relationship, onValueChange = { relationship = it }, label = stringResource(R.string.contacts_relationship))
                Spacer(Modifier.height(8.dp))
                QlmsTextField(value = phone, onValueChange = { phone = it }, label = stringResource(R.string.contacts_phone))
            }
        },
        confirmButton = {
            QlmsPrimaryButton(
                text = stringResource(R.string.action_save),
                enabled = name.isNotBlank() && phone.isNotBlank(),
                onClick = { onSave(EmergencyContact(name = name.trim(), relationship = relationship.trim(), phoneNumber = phone.trim())) },
            )
        },
    )
}

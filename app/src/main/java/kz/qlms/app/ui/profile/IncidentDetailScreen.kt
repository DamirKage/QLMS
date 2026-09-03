package kz.qlms.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.ui.components.FullScreenLoading
import kz.qlms.app.ui.components.QlmsDangerButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(incidentId: String, onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: IncidentDetailViewModel = viewModel(
        key = incidentId,
        factory = viewModelFactory {
            initializer { IncidentDetailViewModel(container.incidentRepository, incidentId) }
        },
    )
    val incident by viewModel.incident.collectAsState()
    val privateDetails by viewModel.privateDetails.collectAsState()
    val currentUid = container.authRepository.currentUser?.uid

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.incident_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        val current = incident
        if (current == null) {
            FullScreenLoading(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(current.incidentType.labelRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(current.status.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(stringResource(R.string.incident_detail_description), current.description.ifBlank { "—" })
                        InfoRow(stringResource(R.string.incident_detail_address), current.address.ifBlank { "—" })
                        InfoRow(
                            stringResource(R.string.incident_detail_coordinates),
                            "${current.latitude}, ${current.longitude}",
                        )
                    }
                }

                val note = privateDetails?.dispatcherNote
                if (!note.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.incident_detail_dispatcher_note), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (current.reporterId == currentUid && current.status.name in listOf("NEW", "ACKNOWLEDGED", "DISPATCHED")) {
                    Spacer(Modifier.height(24.dp))
                    QlmsDangerButton(
                        text = stringResource(R.string.incident_detail_cancel),
                        onClick = { viewModel.cancel(current.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}

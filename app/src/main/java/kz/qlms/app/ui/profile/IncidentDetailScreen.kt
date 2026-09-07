package kz.qlms.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import kz.qlms.app.data.model.IncidentMessage
import kz.qlms.app.data.model.VerificationVote
import kz.qlms.app.ui.components.FullScreenLoading
import kz.qlms.app.ui.components.QlmsDangerButton
import kz.qlms.app.ui.components.QlmsTextField
import kz.qlms.app.ui.theme.QlmsSosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(incidentId: String, onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: IncidentDetailViewModel = viewModel(
        key = incidentId,
        factory = viewModelFactory {
            initializer { IncidentDetailViewModel(container.incidentRepository, container.authRepository, incidentId) }
        },
    )
    val incident by viewModel.incident.collectAsState()
    val privateDetails by viewModel.privateDetails.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val myVote by viewModel.myVerificationVote.collectAsState()
    val currentUid = viewModel.currentUid

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.incident_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(current.status.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (current.isCommunityVerified) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.incident_verified_badge),
                            tint = QlmsSosColors.SuccessContainer,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

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

                val isOwner = current.reporterId == currentUid
                val isActive = current.status.name in listOf("NEW", "ACKNOWLEDGED", "DISPATCHED")

                if (!isOwner) {
                    Spacer(Modifier.height(16.dp))
                    VerificationCard(
                        confirmCount = current.confirmCount,
                        disputeCount = current.disputeCount,
                        myVote = myVote,
                        onVote = viewModel::castVerificationVote,
                    )
                }

                if (isOwner) {
                    Spacer(Modifier.height(16.dp))
                    DispatcherChat(
                        messages = messages,
                        currentUid = currentUid,
                        onSend = viewModel::sendMessage,
                    )
                }

                if (isOwner && isActive) {
                    Spacer(Modifier.height(16.dp))
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
private fun DispatcherChat(
    messages: List<IncidentMessage>,
    currentUid: String?,
    onSend: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.incident_detail_chat_title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            if (messages.isEmpty()) {
                Text(
                    text = stringResource(R.string.incident_detail_chat_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        val fromDispatcher = message.sender == IncidentMessage.Sender.DISPATCHER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (fromDispatcher) Arrangement.Start else Arrangement.End,
                        ) {
                            Surface(
                                color = if (fromDispatcher) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    QlmsTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = stringResource(R.string.incident_detail_chat_hint),
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSend(draft)
                            draft = ""
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.incident_detail_chat_send))
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(
    confirmCount: Int,
    disputeCount: Int,
    myVote: VerificationVote?,
    onVote: (VerificationVote) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.incident_verify_title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.incident_verify_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = myVote == VerificationVote.CONFIRM,
                    onClick = { onVote(VerificationVote.CONFIRM) },
                    leadingIcon = { Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(stringResource(R.string.incident_verify_confirm, confirmCount)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = QlmsSosColors.SuccessContainer),
                )
                FilterChip(
                    selected = myVote == VerificationVote.DISPUTE,
                    onClick = { onVote(VerificationVote.DISPUTE) },
                    leadingIcon = { Icon(Icons.Filled.ThumbDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(stringResource(R.string.incident_verify_dispute, disputeCount)) },
                )
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

package kz.qlms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.qlms.app.R
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentStatus
import kz.qlms.app.ui.theme.QlmsSosColors

@Composable
fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = incident.incidentType.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(incident.incidentType.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (incident.isSosTriggered) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = QlmsSosColors.ActionRed, shape = CircleShape) {
                            Text(
                                "SOS",
                                color = QlmsSosColors.OnActionRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (incident.isCommunityVerified) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.incident_verified_badge),
                            tint = QlmsSosColors.SuccessContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (incident.description.isNotBlank()) {
                    Text(
                        text = incident.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            StatusChip(status = incident.status)
        }
    }
}

@Composable
private fun StatusChip(status: IncidentStatus) {
    val color = when (status) {
        IncidentStatus.NEW -> MaterialTheme.colorScheme.errorContainer
        IncidentStatus.ACKNOWLEDGED, IncidentStatus.DISPATCHED -> MaterialTheme.colorScheme.tertiaryContainer
        IncidentStatus.RESOLVED -> QlmsSosColors.SuccessContainer
        IncidentStatus.FALSE_ALARM, IncidentStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = color, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
        Text(
            text = stringResource(status.labelRes),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

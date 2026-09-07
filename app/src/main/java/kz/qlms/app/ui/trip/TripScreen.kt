package kz.qlms.app.ui.trip

import android.Manifest
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.data.model.Trip
import kz.qlms.app.data.model.TripStatus
import kz.qlms.app.ui.components.QlmsDangerButton
import kz.qlms.app.ui.components.QlmsPrimaryButton
import kz.qlms.app.ui.components.QlmsTextField
import kz.qlms.app.ui.theme.QlmsSosColors
import kz.qlms.app.util.LocationUtils

private val durationPresets = listOf(15L, 30L, 60L, 120L)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val context = LocalContext.current
    val viewModel: TripViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TripViewModel(
                    container.tripRepository,
                    container.userRepository,
                    container.authRepository,
                    context.applicationContext,
                )
            }
        },
    )

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    )
    val activeTrip by viewModel.activeTrip.collectAsState()
    val trip = activeTrip

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (trip != null && trip.status == TripStatus.ACTIVE) {
                ActiveTripContent(
                    trip = trip,
                    onArrived = { viewModel.markArrived(trip.id) },
                    onCancel = { viewModel.cancelTrip(trip.id) },
                )
            } else {
                StartTripContent(
                    isStarting = viewModel.isStarting,
                    hasLocationPermission = locationPermissions.allPermissionsGranted,
                    onRequestLocationPermission = { locationPermissions.launchMultiplePermissionRequest() },
                    onStart = { destination, minutes -> viewModel.startTrip(destination, minutes) },
                )
            }
        }
    }
}

@Composable
private fun StartTripContent(
    isStarting: Boolean,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStart: (String, Long) -> Unit,
) {
    var destination by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableStateOf(durationPresets[1]) }

    Icon(
        Icons.Filled.DirectionsWalk,
        contentDescription = null,
        modifier = Modifier.size(36.dp).padding(bottom = 4.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.trip_explanation),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))

    QlmsTextField(
        value = destination,
        onValueChange = { destination = it },
        label = stringResource(R.string.trip_destination_label),
    )
    Spacer(Modifier.height(20.dp))

    Text(stringResource(R.string.trip_duration_label), style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        durationPresets.forEach { minutes ->
            FilterChip(
                selected = minutes == selectedMinutes,
                onClick = { selectedMinutes = minutes },
                label = { Text(stringResource(R.string.trip_minutes_format, minutes.toString())) },
            )
        }
    }

    Spacer(Modifier.height(32.dp))
    QlmsPrimaryButton(
        text = stringResource(R.string.trip_start_action),
        loading = isStarting,
        enabled = destination.isNotBlank(),
        onClick = {
            if (!hasLocationPermission) onRequestLocationPermission()
            onStart(destination, selectedMinutes)
        },
    )
}

@Composable
private fun ActiveTripContent(trip: Trip, onArrived: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val shareTemplate = stringResource(R.string.trip_share_message)
    val shareTitle = stringResource(R.string.trip_share_link_action)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.trip_active_heading),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.trip_active_destination, trip.destination),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            val statusLabel = when (trip.status) {
                TripStatus.ACTIVE -> R.string.trip_status_active
                TripStatus.ARRIVED -> R.string.trip_status_arrived
                TripStatus.ALERTED -> R.string.trip_status_alerted
                TripStatus.CANCELLED -> R.string.trip_status_cancelled
            }
            val statusColor = if (trip.status == TripStatus.ALERTED) QlmsSosColors.ActionRed else MaterialTheme.colorScheme.primary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = statusColor) {}
                Spacer(Modifier.width(8.dp))
                Text(stringResource(statusLabel), style = MaterialTheme.typography.labelLarge, color = statusColor)
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        onClick = {
            val link = LocationUtils.googleMapsLink(trip.latitude, trip.longitude)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareTemplate.format(trip.destination, link))
            }
            context.startActivity(Intent.createChooser(intent, shareTitle))
        },
    ) {
        Icon(Icons.Filled.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(shareTitle)
    }

    Spacer(Modifier.height(12.dp))
    QlmsPrimaryButton(text = stringResource(R.string.trip_arrived_action_button), onClick = onArrived)
    Spacer(Modifier.height(12.dp))
    QlmsDangerButton(text = stringResource(R.string.trip_cancel_action), onClick = onCancel)
}

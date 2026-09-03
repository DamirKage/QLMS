package kz.qlms.app.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.service.FakeCallScheduler
import kz.qlms.app.service.SosForegroundService
import kz.qlms.app.ui.components.IncidentTypeGrid
import kz.qlms.app.ui.sos.SosButton
import kz.qlms.app.ui.sos.SosConfirmSheet
import kz.qlms.app.ui.theme.QlmsSosColors
import kz.qlms.app.util.LocationUtils
import kz.qlms.app.util.PhoneUtils

private val ALMATY = LatLng(43.238949, 76.889709)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReport: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onIncidentClick: (String) -> Unit,
) {
    val container = rememberAppContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(container.incidentRepository, container.authRepository, container.settingsDataStore.settingsFlow)
            }
        },
    )

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    )
    val sosPermissions = rememberMultiplePermissionsState(
        buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        },
    )
    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    val userLocation by viewModel.userLocation.collectAsState()
    val nearbyIncidents by viewModel.nearbyIncidents.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val sosActive by SosForegroundService.isActive.collectAsState()

    var showTypePicker by remember { mutableStateOf(false) }
    var pendingType by remember { mutableStateOf<IncidentType?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val fakeCallDefaultName = stringResource(R.string.fake_call_default_name)

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            LocationUtils.getCurrentLocation(context)?.let(viewModel::onLocationResolved)
        } else {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ALMATY, 12f)
    }
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationPermissions.allPermissionsGranted),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
        ) {
            nearbyIncidents.forEach { incident ->
                Marker(
                    state = MarkerState(LatLng(incident.latitude, incident.longitude)),
                    title = stringResource(incident.incidentType.labelRes),
                    snippet = incident.description.ifBlank { stringResource(incident.status.labelRes) },
                    onClick = { onIncidentClick(incident.id); true },
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (pendingCount > 0) {
                StatusBanner(
                    icon = Icons.Filled.CloudOff,
                    text = stringResource(R.string.home_pending_sync_banner, pendingCount),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (sosActive) {
                Spacer(Modifier.height(8.dp))
                StatusBanner(
                    icon = Icons.Filled.Warning,
                    text = stringResource(R.string.home_sos_active_banner),
                    color = QlmsSosColors.ActionRed,
                    contentColor = Color.White,
                )
            }
        }

        FloatingActionButton(
            onClick = onNavigateToCheckIn,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(top = 48.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.nav_checkin))
        }

        val fakeCallScheduledText = stringResource(R.string.home_fake_call_scheduled)
        FloatingActionButton(
            onClick = {
                if (!notificationPermission.status.isGranted) {
                    notificationPermission.launchPermissionRequest()
                } else {
                    FakeCallScheduler.schedule(context, callerName = fakeCallDefaultName)
                    scope.launch { snackbarHostState.showSnackbar(fakeCallScheduledText) }
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .padding(top = 48.dp),
        ) {
            Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.home_fake_call_action))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SosButton(onClick = { showTypePicker = true })
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_report_incident_action),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable(onClick = onNavigateToReport),
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showTypePicker) {
        ModalBottomSheet(onDismissRequest = { showTypePicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.sos_pick_type_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                IncidentTypeGrid(
                    selected = null,
                    onSelect = { type ->
                        showTypePicker = false
                        pendingType = type
                    },
                    modifier = Modifier.height(320.dp),
                )
            }
        }
    }

    pendingType?.let { type ->
        SosConfirmSheet(
            incidentType = type,
            onDismiss = { pendingType = null },
            onConfirmed = {
                pendingType = null
                scope.launch {
                    if (!sosPermissions.allPermissionsGranted) sosPermissions.launchMultiplePermissionRequest()
                    SosForegroundService.start(context, type)
                    val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasCallPermission) {
                        PhoneUtils.callEmergencyNumber(context, type.referenceNumber.ifBlank { "112" })
                    } else {
                        PhoneUtils.dialEmergencyNumber(context, type.referenceNumber.ifBlank { "112" })
                    }
                }
            },
        )
    }
}

@Composable
private fun StatusBanner(
    icon: ImageVector,
    text: String,
    color: Color,
    contentColor: Color,
) {
    Surface(color = color, contentColor = contentColor, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

package kz.qlms.app.ui.report

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.ui.components.IncidentTypeGrid
import kz.qlms.app.ui.components.QlmsPrimaryButton
import kz.qlms.app.ui.components.QlmsTextField
import kz.qlms.app.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReportIncidentScreen(onSubmitted: () -> Unit, onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ReportViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReportViewModel(container.incidentRepository, container.authRepository) }
        },
    )

    var selectedType by remember { mutableStateOf<IncidentType?>(null) }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf(listOf<Uri>()) }

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val uiState by viewModel.uiState.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { photos = photos + it }
    }

    LaunchedEffect(uiState) {
        if (uiState is ReportUiState.Submitted) onSubmitted()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.report_pick_type), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            IncidentTypeGrid(
                selected = selectedType,
                onSelect = { selectedType = it },
                modifier = Modifier.height(320.dp),
            )

            Spacer(Modifier.height(16.dp))
            QlmsTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.report_description_label),
                singleLine = false,
                minLines = 3,
            )

            Spacer(Modifier.height(12.dp))
            QlmsTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.report_address_label),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.report_photos_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(84.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { photoPicker.launch("image/*") },
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.report_add_photo))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                Text(stringResource(R.string.report_anonymous_toggle))
            }

            Spacer(Modifier.height(16.dp))
            QlmsPrimaryButton(
                text = stringResource(R.string.report_submit_action),
                loading = uiState is ReportUiState.Submitting,
                enabled = selectedType != null,
                onClick = {
                    val type = selectedType ?: return@QlmsPrimaryButton
                    scope.launch {
                        val location = if (locationPermission.status.isGranted) LocationUtils.getCurrentLocation(context) else null
                        viewModel.submit(
                            type = type,
                            description = description,
                            address = address,
                            isAnonymous = isAnonymous,
                            location = location,
                            localPhotoPaths = photos.map { it.toString() },
                        )
                    }
                },
            )
        }
    }
}

package kz.qlms.app.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.ui.components.QlmsPrimaryButton

private val intervalPresets = listOf(60L to "1", 120L to "2", 240L to "4", 480L to "8", 720L to "12", 1440L to "24")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val context = LocalContext.current
    val viewModel: CheckInViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CheckInViewModel(container.settingsDataStore, context.applicationContext) }
        },
    )
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.checkin_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.checkin_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.checkin_enable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = settings.checkInEnabled,
                    onCheckedChange = { viewModel.setEnabled(it, settings.checkInIntervalMinutes) },
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.checkin_interval_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                intervalPresets.forEach { (minutes, hoursLabel) ->
                    AssistChip(
                        onClick = { viewModel.setEnabled(settings.checkInEnabled, minutes) },
                        label = { Text(stringResource(R.string.checkin_hours_format, hoursLabel)) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            QlmsPrimaryButton(
                text = stringResource(R.string.checkin_im_safe_action),
                onClick = { viewModel.checkInNow() },
                enabled = settings.checkInEnabled,
            )
        }
    }
}

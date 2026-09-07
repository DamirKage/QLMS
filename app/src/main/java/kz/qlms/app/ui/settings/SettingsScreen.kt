package kz.qlms.app.ui.settings

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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kz.qlms.app.core.Constants
import kz.qlms.app.data.model.AppLanguage
import kz.qlms.app.data.model.SosTriggerMode
import kz.qlms.app.data.model.ThemeMode
import kz.qlms.app.ui.components.QlmsDangerButton
import kz.qlms.app.ui.components.QlmsPrimaryButton
import kz.qlms.app.ui.components.QlmsTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, onSignedOut: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsDataStore, container.userRepository, container.authRepository) }
        },
    )
    val settings by viewModel.settings.collectAsState()
    val accountDeleted by viewModel.accountDeleted.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }

    LaunchedEffect(accountDeleted) {
        if (accountDeleted) onSignedOut()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            SectionLabel(stringResource(R.string.settings_section_appearance))
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip(ThemeMode.SYSTEM, settings.themeMode, stringResource(R.string.settings_theme_system), viewModel::setThemeMode)
                ThemeChip(ThemeMode.LIGHT, settings.themeMode, stringResource(R.string.settings_theme_light), viewModel::setThemeMode)
                ThemeChip(ThemeMode.DARK, settings.themeMode, stringResource(R.string.settings_theme_dark), viewModel::setThemeMode)
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_high_contrast))
                Switch(checked = settings.highContrastText, onCheckedChange = viewModel::setHighContrast)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionLabel(stringResource(R.string.settings_section_language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChip(AppLanguage.KAZAKH, settings.language, "Қазақша", viewModel::setLanguage)
                LanguageChip(AppLanguage.RUSSIAN, settings.language, "Русский", viewModel::setLanguage)
                LanguageChip(AppLanguage.ENGLISH, settings.language, "English", viewModel::setLanguage)
            }
            Text(
                text = stringResource(R.string.settings_language_restart_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionLabel(stringResource(R.string.settings_section_safety))
            Text(
                stringResource(R.string.settings_radius_label, settings.nearbyRadiusKm.toInt()),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = settings.nearbyRadiusKm.toFloat(),
                onValueChange = { viewModel.setRadiusKm(it.toDouble()) },
                valueRange = 1f..30f,
                steps = 28,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_silent_sos))
                    Text(
                        stringResource(R.string.settings_silent_sos_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.silentSosEnabled, onCheckedChange = viewModel::setSilentSos)
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_sos_trigger_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.sosTriggerMode == SosTriggerMode.TAP_CONFIRM,
                    onClick = { viewModel.setSosTriggerMode(SosTriggerMode.TAP_CONFIRM) },
                    label = { Text(stringResource(R.string.settings_sos_trigger_tap)) },
                )
                FilterChip(
                    selected = settings.sosTriggerMode == SosTriggerMode.HOLD_TO_ARM,
                    onClick = {
                        if (viewModel.hasSafetyPin()) {
                            viewModel.setSosTriggerMode(SosTriggerMode.HOLD_TO_ARM)
                        } else {
                            showSetPin = true
                        }
                    },
                    label = { Text(stringResource(R.string.settings_sos_trigger_hold)) },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_panic_siren))
                    Text(
                        stringResource(R.string.settings_panic_siren_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.panicSirenEnabled, onCheckedChange = viewModel::setPanicSiren)
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_crash_detection))
                    Text(
                        stringResource(R.string.settings_crash_detection_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.crashDetectionEnabled, onCheckedChange = viewModel::setCrashDetection)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionLabel(stringResource(R.string.settings_section_privacy))
            Text(
                text = stringResource(R.string.settings_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text(stringResource(R.string.settings_delete_account), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showSetPin) {
        var pin by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSetPin = false },
            title = { Text(stringResource(R.string.settings_set_pin_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_set_pin_body, Constants.HOLD_TO_ARM_GRACE_SECONDS),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    QlmsTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = stringResource(R.string.settings_pin_label),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                    )
                }
            },
            confirmButton = {
                QlmsPrimaryButton(
                    text = stringResource(R.string.settings_pin_save_action),
                    enabled = pin.length in 4..6,
                    onClick = {
                        viewModel.saveSafetyPinAndArm(pin)
                        showSetPin = false
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { showSetPin = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
            confirmButton = {
                QlmsDangerButton(
                    text = stringResource(R.string.settings_delete_account),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccountAndData()
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ThemeChip(mode: ThemeMode, current: ThemeMode, label: String, onSelect: (ThemeMode) -> Unit) {
    FilterChip(selected = mode == current, onClick = { onSelect(mode) }, label = { Text(label) })
}

@Composable
private fun LanguageChip(language: AppLanguage, current: AppLanguage, label: String, onSelect: (AppLanguage) -> Unit) {
    FilterChip(selected = language == current, onClick = { onSelect(language) }, label = { Text(label) })
}

package kz.qlms.app.ui.sos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kz.qlms.app.R
import kz.qlms.app.core.Constants
import kz.qlms.app.ui.components.QlmsTextField
import kz.qlms.app.ui.theme.QlmsSosColors

/**
 * Shown right after [HoldToArmButton] reports "armed and released". A short
 * grace window during which entering the correct safety PIN stands the SOS
 * down; letting the countdown reach zero — or getting the PIN wrong and
 * running out of time — fires it, same as the tap+countdown flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinCancelSheet(
    onCorrectPin: () -> Unit,
    onCountdownExpired: () -> Unit,
    checkPin: (String) -> Boolean,
) {
    var secondsLeft by remember { mutableIntStateOf(Constants.HOLD_TO_ARM_GRACE_SECONDS) }
    var pinInput by remember { mutableStateOf("") }
    var wrongPin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive && secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            if (secondsLeft <= 0) onCountdownExpired()
        }
        onDispose { job.cancel() }
    }

    fun submit() {
        if (checkPin(pinInput)) {
            onCorrectPin()
        } else {
            wrongPin = true
            pinInput = ""
        }
    }

    ModalBottomSheet(onDismissRequest = {}) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = QlmsSosColors.ActionRed)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pin_cancel_title, secondsLeft),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.pin_cancel_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            QlmsTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6) { pinInput = it; wrongPin = false } },
                label = stringResource(R.string.pin_cancel_field_label),
                keyboardType = KeyboardType.NumberPassword,
                isError = wrongPin,
                supportingText = if (wrongPin) stringResource(R.string.pin_cancel_wrong) else null,
            )

            Spacer(Modifier.height(16.dp))
            kz.qlms.app.ui.components.QlmsPrimaryButton(
                text = stringResource(R.string.pin_cancel_action),
                onClick = { submit() },
                enabled = pinInput.isNotBlank(),
            )
        }
    }
}

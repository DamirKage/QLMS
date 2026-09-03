package kz.qlms.app.ui.sos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kz.qlms.app.R
import kz.qlms.app.core.Constants
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.ui.components.QlmsDangerButton
import kz.qlms.app.ui.theme.QlmsSosColors

private const val CONFIRMED_ANIMATION_HOLD_MS = 550L

/**
 * Bottom sheet shown after the SOS button is pressed: a short, cancelable
 * countdown so an accidental tap doesn't dispatch real emergency services,
 * then a brief confirmation animation before the actual dispatch fires — the
 * animation isn't decorative, it's the only feedback the user gets that the
 * press registered before the sheet disappears and the call/notification UI
 * takes over.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosConfirmSheet(
    incidentType: IncidentType,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
) {
    var secondsLeft by remember { mutableIntStateOf(Constants.SOS_COUNTDOWN_SECONDS) }
    var confirmed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive && secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            confirmed = true
            delay(CONFIRMED_ANIMATION_HOLD_MS)
            onConfirmed()
        }
        onDispose { job.cancel() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.sos_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sos_confirm_body, stringResource(incidentType.labelRes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                if (confirmed) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(tween(320)) + fadeIn(tween(220)),
                    ) {
                        Box(
                            modifier = Modifier.size(88.dp).background(QlmsSosColors.Success, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        progress = { secondsLeft / Constants.SOS_COUNTDOWN_SECONDS.toFloat() },
                        modifier = Modifier.size(88.dp),
                        color = QlmsSosColors.ActionRed,
                        strokeWidth = 6.dp,
                    )
                    Text(text = "$secondsLeft", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
            if (!confirmed) {
                QlmsDangerButton(text = stringResource(R.string.sos_confirm_cancel), onClick = onDismiss)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

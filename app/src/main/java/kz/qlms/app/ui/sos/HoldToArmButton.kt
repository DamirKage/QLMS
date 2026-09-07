package kz.qlms.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kz.qlms.app.R
import kz.qlms.app.ui.theme.QlmsSosColors

private const val ARM_DURATION_MS = 1100L

/**
 * SafeTrek/Noonlight-style trigger: hold the button until it fills and a
 * vibration confirms it's armed, then release. What happens on release is the
 * caller's job (show the PIN-cancel countdown) — this composable only ever
 * reports "armed and released", never fires anything itself.
 */
@Composable
fun HoldToArmButton(onArmedAndReleased: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                else -> Unit
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            armed = false
            val start = System.currentTimeMillis()
            while (isActive && isPressed) {
                val elapsed = System.currentTimeMillis() - start
                progress = (elapsed.toFloat() / ARM_DURATION_MS).coerceIn(0f, 1f)
                if (progress >= 1f && !armed) {
                    armed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                delay(16)
            }
            if (armed) onArmedAndReleased()
            progress = 0f
            armed = false
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(0.8f + 0.2f * progress)
                .background(QlmsSosColors.ActionRed.copy(alpha = 0.12f + 0.25f * progress), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(QlmsSosColors.ActionRed, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (armed) stringResource(R.string.hold_sos_release_label) else stringResource(R.string.hold_sos_label),
                color = QlmsSosColors.OnActionRed,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

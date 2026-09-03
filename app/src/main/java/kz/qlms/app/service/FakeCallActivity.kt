package kz.qlms.app.service

import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kz.qlms.app.R
import kz.qlms.app.ui.theme.QlmsTheme

/**
 * A convincing, discreet "incoming call" — press-of-a-button exit tool used
 * by several safety apps abroad (bSafe, Companion). Shows over the lock
 * screen like a real call, rings + vibrates, and either button just ends it —
 * the point isn't to fool a phone system, it's to give the user a socially
 * normal reason to step away from a situation.
 */
class FakeCallActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLockedAndTurnScreenOn()
        startRingingAndVibrating()

        setContent {
            QlmsTheme {
                FakeCallScreen(
                    callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: getString(R.string.fake_call_default_name),
                    onEnd = { stopRingingAndVibrating(); finish() },
                )
            }
        }
    }

    private fun setShowWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
    }

    private fun startRingingAndVibrating() {
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                streamType = AudioManager.STREAM_RING
                play()
            }
        }
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 800, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopRingingAndVibrating() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
    }

    override fun onDestroy() {
        stopRingingAndVibrating()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CALLER_NAME = "extra_caller_name"
    }
}

@Composable
private fun FakeCallScreen(callerName: String, onEnd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121316)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Text(
                text = stringResource(R.string.fake_call_incoming_label),
                color = Color(0xFFBFC3CC),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(120.dp).background(Color(0xFF2C2E33), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF9DA2AD), modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(callerName, color = Color.White, style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CallActionButton(icon = Icons.Filled.CallEnd, color = Color(0xFFE0301F), label = stringResource(R.string.fake_call_decline), onClick = onEnd)
                CallActionButton(icon = Icons.Filled.Call, color = Color(0xFF1E7A34), label = stringResource(R.string.fake_call_answer), onClick = onEnd)
            }
        }
    }
}

@Composable
private fun CallActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(color, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color(0xFFBFC3CC), style = MaterialTheme.typography.labelMedium)
    }
}

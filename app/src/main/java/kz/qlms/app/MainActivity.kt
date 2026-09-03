package kz.qlms.app

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.data.model.AppSettings
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.service.SosForegroundService
import kz.qlms.app.ui.QlmsApp
import kz.qlms.app.ui.theme.QlmsTheme
import kz.qlms.app.util.ShakeDetector

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkIncidentId = intent?.getStringExtra(EXTRA_DEEP_LINK_INCIDENT_ID)
            ?: intent?.data?.takeIf { it.host == "incident" }?.lastPathSegment

        setContent {
            val container = rememberAppContainer()
            val settings by container.settingsDataStore.settingsFlow.collectAsState(initial = AppSettings())

            DisposableEffect(settings.silentSosEnabled) {
                val detector = if (settings.silentSosEnabled) {
                    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                    ShakeDetector(sensorManager) {
                        SosForegroundService.start(applicationContext, IncidentType.OTHER)
                    }.also { it.start() }
                } else {
                    null
                }
                onDispose { detector?.stop() }
            }

            QlmsTheme(themeMode = settings.themeMode) {
                QlmsApp(deepLinkIncidentId = deepLinkIncidentId)
            }
        }
    }

    companion object {
        const val EXTRA_DEEP_LINK_INCIDENT_ID = "extra_deep_link_incident_id"
    }
}

package kz.qlms.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kz.qlms.app.MainActivity
import kz.qlms.app.QlmsApplication
import kz.qlms.app.R
import kz.qlms.app.core.Constants
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentPrivateDetails
import kz.qlms.app.data.model.IncidentType
import kz.qlms.app.util.LocationUtils
import kz.qlms.app.util.PhoneUtils

/**
 * Runs for the lifetime of an active SOS: takes an initial fix, writes/queues
 * the incident, notifies emergency contacts by SMS, and keeps pushing location
 * updates to the same Firestore doc every few seconds so a dispatcher watching
 * the web panel sees the reporter moving in near-real time.
 */
class SosForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var incidentId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val typeName = intent?.getStringExtra(EXTRA_INCIDENT_TYPE) ?: IncidentType.OTHER.name
        val description = intent?.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val textOnly = intent?.getBooleanExtra(EXTRA_TEXT_ONLY, false) ?: false
        val impactForceG = intent?.getDoubleExtra(EXTRA_IMPACT_FORCE_G, -1.0)?.takeIf { it >= 0 }

        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.sos_notification_locating)))
        _isActive.value = true

        scope.launch { runSosFlow(typeName, description, textOnly, impactForceG) }
        return START_STICKY
    }

    private suspend fun runSosFlow(typeName: String, description: String, textOnly: Boolean, impactForceG: Double?) {
        val app = applicationContext as QlmsApplication
        val repo = app.container.incidentRepository
        val userRepo = app.container.userRepository
        val auth = app.container.authRepository
        val uid = auth.currentUser?.uid ?: return stopSelfSafely()

        val location = LocationUtils.getCurrentLocation(applicationContext)
        if (location == null) {
            updateNotification(getString(R.string.sos_notification_no_location))
        }

        val medical = userRepo.getCachedMedicalProfile()
        val contacts = userRepo.getCachedContacts()

        val incident = Incident(
            reporterId = uid,
            reporterDisplayName = auth.currentUser?.displayName.orEmpty(),
            isSosTriggered = true,
            type = typeName,
            description = description,
            isTextOnly = textOnly,
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0,
        )
        val privateDetails = IncidentPrivateDetails(
            medicalSnapshot = medical.takeUnless { it.isEmpty },
            contactsSnapshot = contacts,
            impactForceG = impactForceG,
        )

        val settings = app.container.settingsDataStore.settingsFlow.first()
        val amlNumber = PhoneUtils.amlGatewayNumberOrNull(settings.amlSmsEnabled, settings.amlSmsGatewayNumber)
        if (amlNumber != null && location != null) {
            val amlMessage = PhoneUtils.buildAmlLocationSms(getString(R.string.aml_sms_template), location.latitude, location.longitude)
            PhoneUtils.sendSms(amlNumber, amlMessage)
        }

        var syncedIncidentId: String? = null
        when (val result = repo.createIncidentDirectOrQueue(incident, privateDetails)) {
            is QlmsResult.Success -> {
                incidentId = result.data
                syncedIncidentId = result.data
                _currentIncidentId.value = result.data
                updateNotification(getString(R.string.sos_notification_active))
            }
            is QlmsResult.Error -> updateNotification(getString(R.string.sos_notification_queued))
            QlmsResult.Loading -> Unit
        }

        if (location != null && contacts.isNotEmpty()) {
            val link = LocationUtils.googleMapsLink(location.latitude, location.longitude)
            // Only include the live-tracking web link when the incident actually made it
            // to Firestore (the function backing that page has nothing to serve for a
            // still-queued offline report) and a hosting domain is configured.
            val trackingLink = syncedIncidentId
                ?.takeIf { kz.qlms.app.BuildConfig.TRACKING_BASE_URL.isNotBlank() }
                ?.let { "${kz.qlms.app.BuildConfig.TRACKING_BASE_URL}/track.html?id=$it" }
            val message = if (trackingLink != null) {
                getString(R.string.sos_sms_template, link) + " " + getString(R.string.sos_sms_tracking_suffix, trackingLink)
            } else {
                getString(R.string.sos_sms_template, link)
            }
            PhoneUtils.notifyContactsBySms(contacts, message)
        }

        // Keep pushing fresher fixes to the same doc while the SOS stays active.
        while (scope.isActive()) {
            delay(Constants.SOS_LOCATION_UPDATE_INTERVAL_MS)
            val id = incidentId ?: continue
            val fresh = LocationUtils.getCurrentLocation(applicationContext) ?: continue
            repo.updateIncidentLocation(id, fresh.latitude, fresh.longitude)
        }
    }

    private fun CoroutineScope.isActive() = coroutineContext[Job]?.isActive == true

    override fun onDestroy() {
        _isActive.value = false
        _currentIncidentId.value = null
        SirenController.stop(applicationContext)
        scope.cancel()
        super.onDestroy()
    }

    private fun stopSelfSafely() {
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, SosActionReceiver::class.java).setAction(SosActionReceiver.ACTION_CANCEL_SOS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.sos_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.sos_notification_cancel_action), cancelIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val EXTRA_INCIDENT_TYPE = "extra_incident_type"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_TEXT_ONLY = "extra_text_only"
        const val EXTRA_IMPACT_FORCE_G = "extra_impact_force_g"
        private const val NOTIFICATION_ID = 4201

        private val _isActive = MutableStateFlow(false)
        val isActive = _isActive.asStateFlow()

        private val _currentIncidentId = MutableStateFlow<String?>(null)
        val currentIncidentId = _currentIncidentId.asStateFlow()

        fun start(
            context: android.content.Context,
            type: IncidentType,
            description: String = "",
            isTextOnly: Boolean = false,
            impactForceG: Double? = null,
        ) {
            val intent = Intent(context, SosForegroundService::class.java)
                .putExtra(EXTRA_INCIDENT_TYPE, type.name)
                .putExtra(EXTRA_DESCRIPTION, description)
                .putExtra(EXTRA_TEXT_ONLY, isTextOnly)
            if (impactForceG != null) intent.putExtra(EXTRA_IMPACT_FORCE_G, impactForceG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, SosForegroundService::class.java))
        }
    }
}

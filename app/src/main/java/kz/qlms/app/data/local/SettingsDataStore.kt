package kz.qlms.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kz.qlms.app.data.model.AppLanguage
import kz.qlms.app.data.model.AppSettings
import kz.qlms.app.data.model.SosTriggerMode
import kz.qlms.app.data.model.ThemeMode

private val Context.dataStore by preferencesDataStore(name = "qlms_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val RADIUS_KM = doublePreferencesKey("nearby_radius_km")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_text")
        val SILENT_SOS = booleanPreferencesKey("silent_sos_enabled")
        val CHECK_IN_ENABLED = booleanPreferencesKey("check_in_enabled")
        val CHECK_IN_INTERVAL = longPreferencesKey("check_in_interval_minutes")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_CHECK_IN = longPreferencesKey("last_check_in_epoch_ms")
        val CHECK_IN_ALERT_SENT = booleanPreferencesKey("check_in_alert_sent")
        val SOS_TRIGGER_MODE = stringPreferencesKey("sos_trigger_mode")
        val CRASH_DETECTION = booleanPreferencesKey("crash_detection_enabled")
        val PANIC_SIREN = booleanPreferencesKey("panic_siren_enabled")
    }

    val lastCheckInFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_CHECK_IN] ?: System.currentTimeMillis() }

    suspend fun recordCheckIn() {
        context.dataStore.edit {
            it[Keys.LAST_CHECK_IN] = System.currentTimeMillis()
            it[Keys.CHECK_IN_ALERT_SENT] = false
        }
    }

    suspend fun markCheckInAlertSent() {
        context.dataStore.edit { it[Keys.CHECK_IN_ALERT_SENT] = true }
    }

    suspend fun wasCheckInAlertSent(): Boolean =
        context.dataStore.data.first()[Keys.CHECK_IN_ALERT_SENT] ?: false

    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            language = prefs[Keys.LANGUAGE]?.let { tag -> AppLanguage.entries.find { it.tag == tag } } ?: AppLanguage.KAZAKH,
            nearbyRadiusKm = prefs[Keys.RADIUS_KM] ?: 5.0,
            highContrastText = prefs[Keys.HIGH_CONTRAST] ?: false,
            silentSosEnabled = prefs[Keys.SILENT_SOS] ?: false,
            checkInEnabled = prefs[Keys.CHECK_IN_ENABLED] ?: false,
            checkInIntervalMinutes = prefs[Keys.CHECK_IN_INTERVAL] ?: 120L,
            sosTriggerMode = prefs[Keys.SOS_TRIGGER_MODE]?.let { runCatching { SosTriggerMode.valueOf(it) }.getOrNull() } ?: SosTriggerMode.TAP_CONFIRM,
            crashDetectionEnabled = prefs[Keys.CRASH_DETECTION] ?: false,
            panicSirenEnabled = prefs[Keys.PANIC_SIREN] ?: true,
        )
    }

    suspend fun setSosTriggerMode(mode: SosTriggerMode) {
        context.dataStore.edit { it[Keys.SOS_TRIGGER_MODE] = mode.name }
    }

    suspend fun setCrashDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CRASH_DETECTION] = enabled }
    }

    suspend fun setPanicSirenEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PANIC_SIREN] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.tag }
    }

    suspend fun setNearbyRadiusKm(radiusKm: Double) {
        context.dataStore.edit { it[Keys.RADIUS_KM] = radiusKm }
    }

    suspend fun setHighContrastText(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }

    suspend fun setSilentSosEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SILENT_SOS] = enabled }
    }

    suspend fun setCheckIn(enabled: Boolean, intervalMinutes: Long) {
        context.dataStore.edit {
            it[Keys.CHECK_IN_ENABLED] = enabled
            it[Keys.CHECK_IN_INTERVAL] = intervalMinutes
        }
    }
}

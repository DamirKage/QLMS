package kz.qlms.app.data.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * TAP_CONFIRM: the original flow — tap SOS, pick a type, a cancelable countdown.
 * HOLD_TO_ARM: a SafeTrek/Noonlight-style alternative — press and hold the
 * button, release it, then either enter your safety PIN within a few seconds
 * to stand down or the SOS fires automatically. Faster and more discreet
 * (no type picker) at the cost of needing a PIN set up in advance.
 */
enum class SosTriggerMode { TAP_CONFIRM, HOLD_TO_ARM }

enum class AppLanguage(val tag: String) {
    KAZAKH("kk"),
    RUSSIAN("ru"),
    ENGLISH("en"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.KAZAKH,
    val nearbyRadiusKm: Double = 5.0,
    val highContrastText: Boolean = false,
    val silentSosEnabled: Boolean = false,
    val checkInEnabled: Boolean = false,
    val checkInIntervalMinutes: Long = 120L,
    val sosTriggerMode: SosTriggerMode = SosTriggerMode.TAP_CONFIRM,
    val crashDetectionEnabled: Boolean = false,
    val panicSirenEnabled: Boolean = true,
    /** AML-style automatic location SMS: on every SOS, in addition to the 112 call, immediately
     * text a precise GPS fix to this number — mirrors what Advanced Mobile Location does at the
     * carrier level, done at the app level since a citizen app can't touch carrier infrastructure. */
    val amlSmsEnabled: Boolean = false,
    val amlSmsGatewayNumber: String = "",
    /** Default for hold-to-arm/shake/crash triggers, which have no per-incident confirm sheet to ask in. */
    val textOnlySosDefault: Boolean = false,
)

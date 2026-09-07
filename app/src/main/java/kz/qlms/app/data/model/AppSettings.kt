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
)

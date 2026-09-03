package kz.qlms.app.data.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
)

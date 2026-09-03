package kz.qlms.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kz.qlms.app.data.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = QlmsBlue40,
    onPrimary = Color.White,
    primaryContainer = QlmsBlue90,
    onPrimaryContainer = QlmsBlue10,
    secondary = QlmsTeal40,
    onSecondary = Color.White,
    secondaryContainer = QlmsTeal90,
    onSecondaryContainer = QlmsBlue10,
    tertiary = QlmsAmber40,
    tertiaryContainer = QlmsAmber90,
    error = QlmsRed40,
    onError = Color.White,
    errorContainer = QlmsRed90,
    onErrorContainer = QlmsBlue10,
    background = QlmsNeutral99,
    onBackground = QlmsNeutral10,
    surface = QlmsNeutral99,
    onSurface = QlmsNeutral10,
    surfaceVariant = QlmsNeutral95,
    onSurfaceVariant = QlmsNeutral20,
    outline = QlmsNeutral20.copy(alpha = 0.5f),
)

private val DarkColors = darkColorScheme(
    primary = QlmsBlue80,
    onPrimary = QlmsBlue20,
    primaryContainer = QlmsBlue20,
    onPrimaryContainer = QlmsBlue90,
    secondary = QlmsTeal80,
    onSecondary = QlmsBlue10,
    secondaryContainer = QlmsTeal40,
    onSecondaryContainer = QlmsTeal90,
    tertiary = QlmsAmber90,
    error = QlmsRed80,
    onError = QlmsBlue10,
    errorContainer = QlmsRed40,
    onErrorContainer = QlmsRed90,
    background = QlmsNeutral10,
    onBackground = QlmsNeutral90,
    surface = QlmsNeutral10,
    onSurface = QlmsNeutral90,
    surfaceVariant = QlmsNeutral20,
    onSurfaceVariant = QlmsNeutral90,
    outline = QlmsNeutral90.copy(alpha = 0.4f),
)

/** SOS-specific colors that MUST stay legible/urgent in both themes — kept out of the swapped scheme on purpose. */
object QlmsSosColors {
    val ActionRed = QlmsRed50
    val OnActionRed = Color.White
    val Success = QlmsSuccess40
    val SuccessContainer = QlmsSuccess90
}

@Composable
fun QlmsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QlmsTypography,
        content = content,
    )
}

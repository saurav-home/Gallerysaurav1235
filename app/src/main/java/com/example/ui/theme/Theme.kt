package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onTertiary = Color(0xFF492532),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
  )

// --- Custom Theme Color Schemes ---

private val EmeraldDarkColorScheme = darkColorScheme(
  primary = Color(0xFF4DB6AC),
  primaryContainer = Color(0xFF004D40),
  onPrimary = Color(0xFF00251A),
  onPrimaryContainer = Color(0xFFB2DFDB),
  secondary = Color(0xFF80CBC4),
  onSecondary = Color(0xFF00332C),
  tertiary = Color(0xFF80DEEA),
  background = Color(0xFF121817),
  surface = Color(0xFF121817),
  onBackground = Color(0xFFE0F2F1),
  onSurface = Color(0xFFE0F2F1)
)

private val EmeraldLightColorScheme = lightColorScheme(
  primary = Color(0xFF00796B),
  primaryContainer = Color(0xFFB2DFDB),
  onPrimary = Color.White,
  onPrimaryContainer = Color(0xFF00251A),
  secondary = Color(0xFF004D40),
  onSecondary = Color.White,
  tertiary = Color(0xFF006064),
  background = Color(0xFFF0FDFB),
  surface = Color(0xFFF0FDFB),
  onBackground = Color(0xFF00251A),
  onSurface = Color(0xFF00251A)
)

private val RubyDarkColorScheme = darkColorScheme(
  primary = Color(0xFFF06292),
  primaryContainer = Color(0xFF880E4F),
  onPrimary = Color(0xFF2A0010),
  onPrimaryContainer = Color(0xFFF8BBD0),
  secondary = Color(0xFFF48FB1),
  onSecondary = Color(0xFF4A001F),
  tertiary = Color(0xFFFF80AB),
  background = Color(0xFF1A1215),
  surface = Color(0xFF1A1215),
  onBackground = Color(0xFFFCE4EC),
  onSurface = Color(0xFFFCE4EC)
)

private val RubyLightColorScheme = lightColorScheme(
  primary = Color(0xFFC2185B),
  primaryContainer = Color(0xFFF8BBD0),
  onPrimary = Color.White,
  onPrimaryContainer = Color(0xFF2A0010),
  secondary = Color(0xFF880E4F),
  onSecondary = Color.White,
  tertiary = Color(0xFFAD1457),
  background = Color(0xFFFFF1F5),
  surface = Color(0xFFFFF1F5),
  onBackground = Color(0xFF2A0010),
  onSurface = Color(0xFF2A0010)
)

private val SapphireDarkColorScheme = darkColorScheme(
  primary = Color(0xFF64B5F6),
  primaryContainer = Color(0xFF0D47A1),
  onPrimary = Color(0xFF00153D),
  onPrimaryContainer = Color(0xFFBBDEFB),
  secondary = Color(0xFF90CAF9),
  onSecondary = Color(0xFF0D3161),
  tertiary = Color(0xFF80D8FF),
  background = Color(0xFF12161A),
  surface = Color(0xFF12161A),
  onBackground = Color(0xFFE3F2FD),
  onSurface = Color(0xFFE3F2FD)
)

private val SapphireLightColorScheme = lightColorScheme(
  primary = Color(0xFF1976D2),
  primaryContainer = Color(0xFFBBDEFB),
  onPrimary = Color.White,
  onPrimaryContainer = Color(0xFF00153D),
  secondary = Color(0xFF0D47A1),
  onSecondary = Color.White,
  tertiary = Color(0xFF0288D1),
  background = Color(0xFFF1F8FF),
  surface = Color(0xFFF1F8FF),
  onBackground = Color(0xFF00153D),
  onSurface = Color(0xFF00153D)
)

private val AmberDarkColorScheme = darkColorScheme(
  primary = Color(0xFFFFB74D),
  primaryContainer = Color(0xFFE65100),
  onPrimary = Color(0xFF3E1F00),
  onPrimaryContainer = Color(0xFFFFE0B2),
  secondary = Color(0xFFFFCC80),
  onSecondary = Color(0xFF4D2C00),
  tertiary = Color(0xFFFFD54F),
  background = Color(0xFF1A1612),
  surface = Color(0xFF1A1612),
  onBackground = Color(0xFFFFF8E1),
  onSurface = Color(0xFFFFF8E1)
)

private val AmberLightColorScheme = lightColorScheme(
  primary = Color(0xFFF57C00),
  primaryContainer = Color(0xFFFFE0B2),
  onPrimary = Color.White,
  onPrimaryContainer = Color(0xFF3E1F00),
  secondary = Color(0xFFE65100),
  onSecondary = Color.White,
  tertiary = Color(0xFFFFA000),
  background = Color(0xFFFFFDF5),
  surface = Color(0xFFFFFDF5),
  onBackground = Color(0xFF3E1F00),
  onSurface = Color(0xFF3E1F00)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  colorPalette: String = "wallpaper",
  useAmoledMode: Boolean = false,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val baseColorScheme = when (colorPalette) {
    "emerald" -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
    "ruby" -> if (darkTheme) RubyDarkColorScheme else RubyLightColorScheme
    "sapphire" -> if (darkTheme) SapphireDarkColorScheme else SapphireLightColorScheme
    "amber" -> if (darkTheme) AmberDarkColorScheme else AmberLightColorScheme
    else -> {
      // "wallpaper" or default
      if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
      }
    }
  }

  val finalColorScheme = if (darkTheme && useAmoledMode) {
    baseColorScheme.copy(
      background = Color.Black,
      surface = Color.Black,
      surfaceVariant = Color(0xFF121212),
      onBackground = Color.White,
      onSurface = Color.White
    )
  } else {
    baseColorScheme
  }

  MaterialTheme(colorScheme = finalColorScheme, typography = Typography, content = content)
}

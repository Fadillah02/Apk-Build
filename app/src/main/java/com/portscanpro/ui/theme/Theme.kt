package com.portscanpro.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryGreen = Color(0xFF00E676)
val PrimaryDark = Color(0xFF00B248)
val Secondary = Color(0xFF1A237E)
val Background = Color(0xFF0D0D0D)
val Surface = Color(0xFF1A1A1A)
val OnBackground = Color(0xFFE0E0E0)
val OnSurface = Color(0xFFE0E0E0)
val ErrorRed = Color(0xFFCF6679)
val WarningOrange = Color(0xFFFFB300)
val SuccessGreen = Color(0xFF00E676)
val CardBg = Color(0xFF222222)
val TerminalGreen = Color(0xFF00FF41)
val AccentBlue = Color(0xFF448AFF)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = ErrorRed,
    surfaceVariant = CardBg,
    outline = Color(0xFF333333)
)

@Composable
fun PortScanProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}

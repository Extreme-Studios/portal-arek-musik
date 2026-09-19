package com.extremestudios.portalarekmusik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColors = darkColorScheme(
    primary = Color(0xFFC5A358), // Gold
    onPrimary = Color(0xFF0B0B0F),
    primaryContainer = Color(0xFF3E321A),
    onPrimaryContainer = Color(0xFFEBD4A1),
    secondary = Color(0xFF4A90E2), // Blue
    onSecondary = Color.White,
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFE1E1E1),
    surface = Color(0xFF16161D),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF25252E),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF44444F)
)

@Composable
fun PortalArekMusikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumDarkColors,
        typography = Typography,
        content = content
    )
}

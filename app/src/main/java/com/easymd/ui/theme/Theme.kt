package com.easymd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SharpShapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00332C),
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    background = Color(0xFFF0F0F0),
    onBackground = Color(0xFF37474F),
    surface = Color.White,
    onSurface = Color(0xFF37474F),
    surfaceVariant = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFF888888),
    outline = Color(0xFFB0BEC5),
    outlineVariant = Color(0xFFE0E0E0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF00332C),
    background = Color(0xFF111111),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF3A3A3A)
)

@Composable
fun EasyMDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SharpShapes,
        content = content
    )
}

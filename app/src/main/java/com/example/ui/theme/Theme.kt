package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Slate950 = Color(0xFF0C1324)
val Slate900 = Color(0xFF151B2D)
val Slate800 = Color(0xFF2E3447)
val Slate700 = Color(0xFF3C494C)
val Slate600 = Color(0xFF6B7A7E)
val Slate500 = Color(0xFF859397)
val Slate400 = Color(0xFFBBC9CD)
val Slate300 = Color(0xFFDCE1FB)
val Slate200 = Color(0xFFE4E8FA)
val Slate100 = Color(0xFFEEF0FF)

val Cyan950 = Color(0xFF00363E)
val Cyan400 = Color(0xFF8AEBFF)
val Cyan300 = Color(0xFFA2EEFF)
val Cyan200 = Color(0xFFB2EBF2)

val Emerald400 = Color(0xFF45DFA4)
val Emerald950 = Color(0xFF003823)

val Amber400 = Color(0xFFFFC107)
val Amber950 = Color(0xFF452B00)
val Amber200 = Color(0xFFFFE082)
val Amber100 = Color(0xFFFFECB3)

val Red400 = Color(0xFFFF9D94)
val Red500 = Color(0xFFEF4444)
val Red950 = Color(0xFF3E0A06)

private val DarkColorScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = Cyan950,
    primaryContainer = Cyan950,
    onPrimaryContainer = Cyan300,
    secondary = Slate400,
    onSecondary = Slate950,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate300,
    tertiary = Emerald400,
    onTertiary = Emerald950,
    background = Slate950,
    onBackground = Slate300,
    surface = Slate900,
    onSurface = Slate300,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = Red400,
    onError = Red950
)

@Composable
fun NillBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

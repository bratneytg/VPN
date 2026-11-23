package com.bratneytg.worldvpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF4ADE80)
val Amber = Color(0xFFFBBF24)
val Red = Color(0xFFF87171)

private val Background = Color(0xFF0B1220)
private val Surface = Color(0xFF111A2E)
private val SurfaceVariant = Color(0xFF1B2640)

private val Colors = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF052E16),
    background = Background,
    onBackground = Color(0xFFE5E9F2),
    surface = Surface,
    onSurface = Color(0xFFE5E9F2),
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFF9AA6C0),
    error = Red,
    onError = Color(0xFF450A0A),
    outline = Color(0xFF3A476A),
)

@Composable
fun WorldVpnTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

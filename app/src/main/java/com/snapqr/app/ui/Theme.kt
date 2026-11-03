package com.snapqr.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.snapqr.app.scan.ScanYellow

private val Colors = darkColorScheme(
    primary = ScanYellow,
    onPrimary = Color.Black,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    background = Color.Black,
    onBackground = Color.White,
)

@Composable
fun SnapQrTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

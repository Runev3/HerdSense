package com.herdsense.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandRed = Color(0xFFD0202E)
val BrandRedDark = Color(0xFFA5141F)
val ScreenBg = Color(0xFFF3F3F5)
val TextMuted = Color(0xFF7A7A7A)
val DividerColor = Color(0xFFE4E4E4)

private val scheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = BrandRedDark,
    secondary = Color(0xFF424242),
    background = ScreenBg,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    surfaceTint = Color.White,
)

@Composable
fun HerdSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

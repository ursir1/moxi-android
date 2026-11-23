package com.moxi.app.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val ScreenGutter = 16.dp
val BottomBarSpace = 96.dp

val StremioBackground = Color(0xFF080D0A)
val SearchBackground = Color(0xFF131D17)
val CardFallback = Color(0xFF101814)
val AccentPurple = Color(0xFF00E676) // Overriding Stremio purple with Moxi Emerald
val MutedText = Color(0xFF94A89C)
val GlassSurface = Color(0xCC111B15)

val AccentGreen = Color(0xFF00E676)
val MoxiGold = Color(0xFFFFD700)

val StremioBackgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF070B09), Color(0xFF0C140F), Color(0xFF060907)),
)

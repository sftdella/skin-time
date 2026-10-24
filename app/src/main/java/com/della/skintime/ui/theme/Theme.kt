package com.della.skintime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SkinTimeColorScheme = lightColorScheme(
    primary          = PinkPrimary,
    onPrimary        = White,
    primaryContainer = PinkLight,
    secondary        = PinkDark,
    onSecondary      = White,
    background       = PinkBackground,
    onBackground     = TextPrimary,
    surface          = White,
    onSurface        = TextPrimary
)

@Composable
fun SkinTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkinTimeColorScheme,
        typography  = Typography,
        content     = content
    )
}

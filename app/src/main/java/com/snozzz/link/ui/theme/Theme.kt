package com.snozzz.link.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blush,
    secondary = MintCandy,
    tertiary = PeachSorbet,
    background = WhitePetal,
    surface = WhitePetal,
    onPrimary = PlumInk,
    onSecondary = PlumInk,
    onTertiary = PlumInk,
    onBackground = PlumInk,
    onSurface = PlumInk,
)

@Composable
fun LinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = LinkTypography,
        content = content,
    )
}

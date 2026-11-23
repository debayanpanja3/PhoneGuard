package com.phoneguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PhoneGuardColors = darkColorScheme(
    primary = Accent,
    secondary = AccentSoft,
    background = BgDeep,
    surface = BgCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Danger
)

@Composable
fun PhoneGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhoneGuardColors,
        typography = PhoneGuardTypography,
        content = content
    )
}

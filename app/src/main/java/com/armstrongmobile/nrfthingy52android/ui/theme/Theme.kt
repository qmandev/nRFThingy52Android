package com.armstrongmobile.nrfthingy52android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// nordicBlue is the brand primary in both light and dark, matching the iOS app, which tints with
// .nordicBlue in both appearances (plan §7.1) — there are no separate dark-variant brand hexes in
// the iOS source. White reads well on the mid-cyan primary and the coral error. Every other role is
// left to the Material 3 defaults, which supply the correct light-vs-dark surfaces and backgrounds.
// Material You dynamic color is intentionally not used: it would replace the Nordic brand accent.
private val LightColorScheme = lightColorScheme(
    primary = NordicColors.nordicBlue,
    onPrimary = Color.White,
    error = NordicColors.nordicRed,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = NordicColors.nordicBlue,
    onPrimary = Color.White,
    error = NordicColors.nordicRed,
    onError = Color.White,
)

@Composable
fun ThingyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}

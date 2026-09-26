package com.aripd.kodokur.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Viewfinder corners and scan line; the same amber as the icon. */
val Amber = Color(0xFFFFB347)

private val Light = lightColorScheme(
    primary = Color(0xFF0F5F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6ECE6),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A6360),
    secondaryContainer = Color(0xFFCCE8E3),
    tertiary = Color(0xFF855400),
    background = Color(0xFFF7FAF9),
    surface = Color(0xFFF7FAF9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8ED4CC),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF0F3D3E),
    onPrimaryContainer = Color(0xFFB6ECE6),
    secondary = Color(0xFFB0CCC7),
    secondaryContainer = Color(0xFF324B48),
    tertiary = Amber,
    background = Color(0xFF101414),
    surface = Color(0xFF101414),
)

/** Uses wallpaper colors on Android 12+; our own palette before that. */
@Composable
fun KodokurTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

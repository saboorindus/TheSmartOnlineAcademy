package com.echologics.thesmartonlineacademy.ui.common.theme

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


val Purple = Color(0xFF534AB7)
val PurpleLight = Color(0xFFEEEDFE)
val PurpleDark = Color(0xFF3C3489)
val Teal = Color(0xFF1D9E75)
val TealLight = Color(0xFFE1F5EE)
val Amber = Color(0xFFBA7517)
val AmberLight = Color(0xFFFAEEDA)
val Gray100 = Color(0xFFF1EFE8)
val Gray600 = Color(0xFF5F5E5A)
val ErrorRed = Color(0xFFE24B4A)
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF2C2C2A)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = White,
    primaryContainer = PurpleLight,
    onPrimaryContainer = PurpleDark,
    secondary = Teal,
    onSecondary = White,
    secondaryContainer = TealLight,
    background = White,
    surface = White,
    onBackground = Black,
    onSurface = Black,
    error = ErrorRed,
    outline = Color(0xFFD3D1C7)
)

@Composable
fun TheSmartOnlineAcademyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
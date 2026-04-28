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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.CompositionLocalProvider

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
    background = Black,
    surface = Black,
    onPrimary = White,
    onBackground = White,
    onSurface = White,
    error = ErrorRed,
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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {

    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColors
    }

    CompositionLocalProvider(
        // 🔥 This fixes edge-to-edge globally for ALL Scaffolds
        androidx.compose.material3.LocalContentColor provides colorScheme.onBackground
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Optional but recommended: use this Scaffold everywhere instead of Material Scaffold
 */
//@Composable
//fun AppScaffold(
//    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
//    topBar: @Composable () -> Unit = {},
//    bottomBar: @Composable () -> Unit = {},
//    content: @Composable (androidx.compose.ui.Modifier) -> Unit
//) {
//    androidx.compose.material3.Scaffold(
//        modifier = modifier,
//        topBar = topBar,
//        bottomBar = bottomBar,
//        contentWindowInsets = WindowInsets.systemBars,
//    ) { padding ->
//        content(androidx.compose.ui.Modifier.padding(padding))
//    }
//}

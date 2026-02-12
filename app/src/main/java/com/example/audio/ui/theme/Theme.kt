package com.example.audio.ui.theme

import android.app.Activity
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

// Schéma de couleurs pour le thème sombre - design moderne pour lecteur audio
private val DarkColorScheme = darkColorScheme(
    primary = MusicPrimary,
    secondary = MusicSecondary,
    tertiary = MusicAccent,
    background = MusicBackground,
    surface = MusicSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0)
)

// Schéma de couleurs pour le thème clair (optionnel, on privilégie le mode sombre)
private val LightColorScheme = lightColorScheme(
    primary = MusicPrimary,
    secondary = MusicSecondary,
    tertiary = MusicAccent,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B)
)

@Composable
fun AudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // On désactive les couleurs dynamiques pour garder notre identité visuelle
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,              // Swiggy Orange
    primaryContainer     = Color(0xFF4A2500),         // Deep warm container
    onPrimaryContainer   = DarkPrimary,
    secondary            = DarkSecondary,             // Soft orange glow
    secondaryContainer   = Color(0xFF332010),
    onSecondaryContainer = DarkSecondary,
    tertiary             = DarkTertiary,              // Swiggy Green
    background           = DarkBackground,            // Charcoal Black
    surface              = DarkSurface,               // Charcoal surface
    surfaceVariant       = Color(0xFF282828),          // Lighter grey variant
    onPrimary            = DarkOnPrimary,             // White
    onSecondary          = Color.White,
    onBackground         = DarkOnBackground,          // Soft white text
    onSurface            = DarkOnSurface,             // Soft white text
    onSurfaceVariant     = Color(0xFFB3B3B3),         // Muted grey text
    outline              = Color(0xFF3C3C3C),
    outlineVariant       = Color(0xFF4C4C4C),
    error                = StatusError,
    onError              = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,             // Swiggy Orange
    primaryContainer     = Color(0xFFFFEBDE),        // Very soft orange chip bg
    onPrimaryContainer   = LightPrimary,
    secondary            = LightSecondary,           // Swiggy Charcoal
    secondaryContainer   = Color(0xFFF0F0F0),        // Soft grey chip bg
    onSecondaryContainer = LightSecondary,
    tertiary             = LightTertiary,            // Swiggy Green
    background           = LightBackground,          // Soft Cream
    surface              = LightSurface,             // Crisp white cards
    surfaceVariant       = Color(0xFFEEEEEE),        // Lighter variant
    onPrimary            = LightOnPrimary,           // White
    onSecondary          = LightOnSecondary,         // White
    onBackground         = LightOnBackground,        // Charcoal Text
    onSurface            = LightOnSurface,           // Charcoal Text
    onSurfaceVariant     = Color(0xFF7E808C),         // Swiggy secondary text (slate grey)
    outline              = Color(0xFFE2E2E2),
    outlineVariant       = Color(0xFFD3D3D3),
    error                = StatusError,
    onError              = Color.White,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

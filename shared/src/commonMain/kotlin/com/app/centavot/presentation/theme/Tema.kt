package com.app.centavot.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cada color significa una sola cosa en toda la app:
// - Verde (primary): plata a favor o algo que salió bien (ventas, ganancia, guardado, cobrado, tope sin problemas).
// - Ámbar (tertiary): atención, solo para avisos de tope (80 % y 90 %).
// - Rojo (error): tope alcanzado o una acción sin vuelta atrás (eliminar).
// - Superficies neutras (surfaceContainer*): todo lo informativo (encuestas, ayuda, privacidad, lo que te deben).
private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F1D4),
    onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF4C6358),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE9DA),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF5FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF6),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9EFEA),
    surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDEE4DF),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C1),
)

/** Siempre claro, aunque el teléfono esté en modo oscuro: todas las pantallas se ven con fondo blanco. */
@Composable
fun CentavotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        content = content,
    )
}

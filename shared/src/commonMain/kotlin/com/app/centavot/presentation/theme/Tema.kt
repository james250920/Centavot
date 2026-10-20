package com.app.centavot.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.app.centavot.resources.Res
import com.app.centavot.resources.sora_bold
import com.app.centavot.resources.sora_light
import com.app.centavot.resources.sora_medium
import com.app.centavot.resources.sora_regular
import com.app.centavot.resources.sora_semibold
import org.jetbrains.compose.resources.Font

// Paleta Andina del manual de marca: Tinta, Lana, Maíz y Carmín. Cada color significa una sola cosa:
// - Tinta (primary): texto, botones principales y avance normal de una barra.
// - Maíz (contenedores): lo elegido y lo de la marca (modo activo, botones tonales, la pestaña activa).
// - Ámbar oscuro (tertiary): atención, solo para avisos de tope (80 % y 90 %) y cobros antiguos.
// - Carmín (error): tope alcanzado, pasarse del presupuesto o una acción sin vuelta atrás.
// - Lana y superficies cálidas (surfaceContainer*): todo lo informativo.
private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1B1410),
    onPrimary = Color(0xFFFAF6EF),
    primaryContainer = Color(0xFFF4B400),
    onPrimaryContainer = Color(0xFF1B1410),
    secondary = Color(0xFF6B5D52),
    onSecondary = Color(0xFFFAF6EF),
    secondaryContainer = Color(0xFFFCE7A8),
    onSecondaryContainer = Color(0xFF1B1410),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFAF6EF),
    tertiaryContainer = Color(0xFFFCE7A8),
    onTertiaryContainer = Color(0xFF2E1F00),
    error = Color(0xFFC1121F),
    onError = Color(0xFFFAF6EF),
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410004),
    background = Color(0xFFFAF6EF),
    onBackground = Color(0xFF1B1410),
    surface = Color(0xFFFAF6EF),
    onSurface = Color(0xFF1B1410),
    surfaceVariant = Color(0xFFEDE4D7),
    onSurfaceVariant = Color(0xFF6B5D52),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4EEE4),
    surfaceContainer = Color(0xFFEFE8DC),
    surfaceContainerHigh = Color(0xFFE9E1D3),
    surfaceContainerHighest = Color(0xFFE3DACB),
    outline = Color(0xFF8C7D70),
    outlineVariant = Color(0xFFDCD1C2),
)

/** Sora en toda la app, como pide el manual: semibold en títulos, regular en textos. */
@Composable
private fun tipografiaSora(): Typography {
    val sora = FontFamily(
        Font(Res.font.sora_light, FontWeight.Light),
        Font(Res.font.sora_regular, FontWeight.Normal),
        Font(Res.font.sora_medium, FontWeight.Medium),
        Font(Res.font.sora_semibold, FontWeight.SemiBold),
        Font(Res.font.sora_bold, FontWeight.Bold),
    )
    return remember(sora) {
        val base = Typography()
        fun TextStyle.conSora(peso: FontWeight? = fontWeight) = copy(fontFamily = sora, fontWeight = peso)
        base.copy(
            displayLarge = base.displayLarge.conSora(),
            displayMedium = base.displayMedium.conSora(),
            displaySmall = base.displaySmall.conSora(),
            headlineLarge = base.headlineLarge.conSora(FontWeight.SemiBold),
            headlineMedium = base.headlineMedium.conSora(FontWeight.SemiBold),
            headlineSmall = base.headlineSmall.conSora(FontWeight.SemiBold),
            titleLarge = base.titleLarge.conSora(FontWeight.SemiBold),
            titleMedium = base.titleMedium.conSora(FontWeight.SemiBold),
            titleSmall = base.titleSmall.conSora(FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.conSora(),
            bodyMedium = base.bodyMedium.conSora(),
            bodySmall = base.bodySmall.conSora(),
            labelLarge = base.labelLarge.conSora(),
            labelMedium = base.labelMedium.conSora(),
            labelSmall = base.labelSmall.conSora(),
        )
    }
}

/** Siempre claro, aunque el teléfono esté en modo oscuro: todas las pantallas se ven con fondo Lana. */
@Composable
fun CentavotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = tipografiaSora(),
        content = content,
    )
}

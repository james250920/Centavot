package com.app.centavot.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.centavot.resources.Res
import com.app.centavot.resources.logo_centavot
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** Ancho entre alto del logotipo del manual de marca (2400 × 404). */
private const val PROPORCION_LOGOTIPO = 2400f / 404f

/**
 * El logotipo de Centavot en su versión Tinta, para fondos claros: la palabra con la moneda quipu en la "o".
 *
 * Va tal cual el manual de marca: sin teñir, sin girar y sin deformar. Con [animado] aparece de a poco y
 * crece apenas hasta su tamaño.
 */
@Composable
fun LogoCentavot(
    modifier: Modifier = Modifier,
    ancho: Dp = 200.dp,
    animado: Boolean = false,
) {
    val escala = remember { Animatable(if (animado) 0.92f else 1f) }
    val opacidad = remember { Animatable(if (animado) 0f else 1f) }
    if (animado) {
        LaunchedEffect(Unit) {
            launch { opacidad.animateTo(1f, tween(450)) }
            escala.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
    }
    Image(
        painter = painterResource(Res.drawable.logo_centavot),
        contentDescription = "Centavot",
        modifier = modifier
            .width(ancho)
            .height(ancho / PROPORCION_LOGOTIPO)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
                alpha = opacidad.value
            },
    )
}

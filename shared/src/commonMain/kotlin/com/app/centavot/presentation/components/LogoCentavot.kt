package com.app.centavot.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.centavot.resources.Res
import com.app.centavot.resources.logo_centavot
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Logo de Centavot: la mano con la moneda de "Un Nuevo Sol".
 *
 * El dibujo es de una sola tinta, así que se pinta con el color del texto y se ve
 * igual en tema claro y oscuro. Con [animado], imita el ícono animado: empieza
 * cerca de la moneda, se aleja girando un poco y luego se mece suave.
 */
/** Lo justo para que se reconozca la mano con la moneda sin competir con el texto encima. */
private const val OPACIDAD_MARCA_DE_AGUA = 0.10f

@Composable
fun LogoCentavot(
    modifier: Modifier = Modifier,
    tamano: Dp = 96.dp,
    animado: Boolean = false,
    /** Como marca de agua: muy tenue y decorativo (TalkBack no lo lee). */
    marcaDeAgua: Boolean = false,
) {
    val escala = remember { Animatable(if (animado) 1.8f else 1f) }
    val giro = remember { Animatable(if (animado) -14f else 0f) }
    val opacidad = remember { Animatable(if (animado) 0f else 1f) }
    // El vaivén entra de a poco al terminar la entrada, sin saltos.
    val amplitud = remember { Animatable(0f) }
    if (animado) {
        LaunchedEffect(Unit) {
            launch { opacidad.animateTo(1f, tween(350)) }
            launch { escala.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            giro.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            amplitud.animateTo(1f, tween(800))
        }
    }
    val vaiven = if (animado) {
        rememberInfiniteTransition(label = "vaiven").animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "giro",
        ).value
    } else {
        0f
    }
    Image(
        painter = painterResource(Res.drawable.logo_centavot),
        contentDescription = if (marcaDeAgua) null else "Centavot",
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .size(tamano)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
                rotationZ = giro.value + vaiven * amplitud.value
                alpha = opacidad.value * if (marcaDeAgua) OPACIDAD_MARCA_DE_AGUA else 1f
            },
    )
}

package com.app.centavot.presentation.screens.compartido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.repository.LectorDeBoletas
import com.app.centavot.domain.repository.LecturaBoleta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Lo que llega desde "Compartir" de otra app (una constancia de Yape, una foto de boleta) y lo leído, de paso entre
 * pantallas. La imagen la deja MainActivity; el borrador lo toma Nuevo gasto o Nueva venta según corresponda.
 */
class BandejaCompartidos {
    val imagen = MutableStateFlow<ByteArray?>(null)
    private var borrador: LecturaBoleta.Leida? = null

    fun dejarBorrador(leida: LecturaBoleta.Leida) { borrador = leida }

    /** Entrega el borrador (y lo saca de la bandeja) solo si es del tipo que pide la pantalla. */
    fun tomarBorrador(ingreso: Boolean): LecturaBoleta.Leida? =
        borrador?.takeIf { it.esIngreso == ingreso }?.also { borrador = null }
}

enum class DestinoCompartido { GASTO, INGRESO }

data class CompartidoUiState(val leyendo: Boolean = true, val error: String? = null, val destino: DestinoCompartido? = null)

class CompartidoViewModel(private val bandeja: BandejaCompartidos, private val lector: LectorDeBoletas) : ViewModel() {
    private val _estado = MutableStateFlow(CompartidoUiState())
    val estado = _estado.asStateFlow()

    init {
        val imagen = bandeja.imagen.value
        bandeja.imagen.value = null
        if (imagen == null) {
            _estado.value = CompartidoUiState(leyendo = false, error = "No llegó ninguna imagen. Vuelve a compartirla.")
        } else {
            viewModelScope.launch {
                when (val lectura = lector.leer(imagen)) {
                    is LecturaBoleta.Leida -> {
                        bandeja.dejarBorrador(lectura)
                        _estado.update { it.copy(leyendo = false, destino = if (lectura.esIngreso) DestinoCompartido.INGRESO else DestinoCompartido.GASTO) }
                    }
                    is LecturaBoleta.Error -> _estado.update { it.copy(leyendo = false, error = lectura.mensaje) }
                }
            }
        }
    }
}

/** "Leyendo la constancia…" y, al terminar, abre Nuevo gasto o Nueva venta ya llenos. Si falla, deja anotar a mano. */
@Composable
fun CompartidoScreen(
    onGasto: () -> Unit,
    onIngreso: () -> Unit,
    onCerrar: () -> Unit,
    viewModel: CompartidoViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.destino) {
        when (estado.destino) {
            DestinoCompartido.GASTO -> onGasto()
            DestinoCompartido.INGRESO -> onIngreso()
            null -> Unit
        }
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val error = estado.error
        if (error == null) {
            CircularProgressIndicator()
            Text("Leyendo la constancia…", style = MaterialTheme.typography.titleMedium)
            Text("Puede tardar hasta un minuto.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        } else {
            Text(error, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Button(onClick = onGasto, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Anotar un gasto a mano") }
            OutlinedButton(onClick = onIngreso, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Anotar una venta o ingreso a mano") }
            TextButton(onClick = onCerrar) { Text("Volver") }
        }
    }
}

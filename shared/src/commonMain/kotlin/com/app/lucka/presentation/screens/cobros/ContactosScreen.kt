package com.app.lucka.presentation.screens.cobros

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.lucka.domain.model.Monto
import com.app.lucka.presentation.components.DialogoConfirmar
import com.app.lucka.presentation.components.DialogoNuevoContacto
import com.app.lucka.presentation.components.EstadoVacio
import com.app.lucka.presentation.components.Iconos
import com.app.lucka.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactosScreen(
    onCerrar: () -> Unit,
    viewModel: ContactosViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dialogos = estado.dialogos

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis contactos") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
            )
        },
        floatingActionButton = {
            if (estado.contactos.isNotEmpty()) {
                FloatingActionButton(onClick = viewModel::pedirAgregar) {
                    Icon(Iconos.Agregar, contentDescription = "Agregar contacto")
                }
            }
        },
    ) { padding ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            estado.contactos.isEmpty() -> EstadoVacio(
                titulo = "Aún no tienes contactos",
                mensaje = "Agrega a las personas a las que les fías o prestas para llevar la cuenta de lo que te deben.",
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = viewModel::pedirAgregar, modifier = Modifier.padding(top = 8.dp)) { Text("Agregar contacto") }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(estado.contactos, key = { it.contacto.id }) { (contacto, debe) ->
                    ListItem(
                        headlineContent = { Text(contacto.nombre) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    contacto.telefono,
                                    if (debe > Monto.CERO) "Te debe ${debe.formatear()}" else "No te debe nada",
                                ).joinToString(" · "),
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.pedirEliminar(contacto) }) {
                                Icon(
                                    Iconos.Eliminar,
                                    contentDescription = "Eliminar a ${contacto.nombre}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    if (dialogos.agregando) {
        DialogoNuevoContacto(error = dialogos.errorNuevo, onGuardar = viewModel::agregar, onCancelar = viewModel::cancelarAgregar)
    }
    dialogos.porEliminar?.let { contacto ->
        DialogoConfirmar(
            titulo = "¿Eliminar a ${contacto.nombre}?",
            mensaje = "Solo se puede eliminar un contacto sin cobros registrados.",
            textoConfirmar = "Eliminar",
            onConfirmar = viewModel::confirmarEliminar,
            onCancelar = viewModel::cancelarEliminar,
        )
    }
    dialogos.aviso?.let { aviso ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarAviso,
            title = { Text("No se puede eliminar") },
            text = { Text(aviso) },
            confirmButton = { TextButton(onClick = viewModel::cerrarAviso) { Text("Entendido") } },
        )
    }
}

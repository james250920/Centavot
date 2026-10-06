package com.app.centavot.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val MAX_NOMBRE = 40
private const val MAX_TELEFONO = 15

/** Pide nombre y teléfono (opcional) de un contacto nuevo. [error] lo decide el ViewModel. */
@Composable
fun DialogoNuevoContacto(
    error: String?,
    onGuardar: (nombre: String, telefono: String) -> Unit,
    onCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo contacto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(MAX_NOMBRE) },
                    label = { Text("Nombre") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { nuevo -> telefono = nuevo.filter { it.isDigit() || it == '+' || it == ' ' }.take(MAX_TELEFONO) },
                    label = { Text("Celular (opcional)") },
                    supportingText = { Text("Para recordarle por WhatsApp lo que te debe.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(nombre, telefono) }) { Text("Agregar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoConfirmar(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    peligroso: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = if (peligroso) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) { Text(textoConfirmar) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

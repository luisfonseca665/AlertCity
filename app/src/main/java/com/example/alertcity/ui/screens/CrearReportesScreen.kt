package com.example.alertcity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReportesScreen(onGuardar: (String, String, String, Boolean, String) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var urgente by remember { mutableStateOf(false) }
    var categoria by remember { mutableStateOf("Baches") }
    val categorias = listOf("Baches", "Alumbrado", "Fugas")

    val timeState = rememberTimePickerState()
    var mostrarTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(text = "Nuevo Reporte", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = titulo, onValueChange = { titulo = it },
            label = { Text("Incidencia") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = descripcion, onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth(), minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { mostrarTimePicker = true }) {
            Text("Seleccionar Hora: ${String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Categoría:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            categorias.forEach { cat ->
                RadioButton(selected = (categoria == cat), onClick = { categoria = cat })
                Text(cat, modifier = Modifier.padding(end = 8.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = urgente, onCheckedChange = { urgente = it })
            Text("Marcar como urgente")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val horaFmt = String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)
                onGuardar(titulo, descripcion, categoria, urgente, horaFmt)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = titulo.isNotBlank() && descripcion.isNotBlank()
        ) {
            Text("Guardar Reporte")
        }
    }

    if (mostrarTimePicker) {
        AlertDialog(
            onDismissRequest = { mostrarTimePicker = false },
            confirmButton = { TextButton(onClick = { mostrarTimePicker = false }) { Text("OK") } },
            text = { TimePicker(state = timeState) }
        )
    }
}
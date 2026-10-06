package com.example.alertcity.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale

fun crearUriTemporal(context: Context): Uri {
    val directorio = File(context.cacheDir, "images")
    directorio.mkdirs()

    val archivo = File(directorio, "evidencia_${System.currentTimeMillis()}.jpg")
    archivo.createNewFile()

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        archivo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReportesScreen(onGuardar: (String, String, String, Boolean, String, String?, String?) -> Unit) {
    val context = LocalContext.current

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var urgente by remember { mutableStateOf(false) }
    var categoria by remember { mutableStateOf("Baches") }
    val categorias = listOf("Baches", "Alumbrado", "Fugas")

    val timeState = rememberTimePickerState()
    var mostrarTimePicker by remember { mutableStateOf(false) }

    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarOpcionesFoto by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) fotoUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) fotoUri = tempUri
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            ubicacion = "20.1387° N, -101.1895° W" // Simulación de coordenadas
        }
    }

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
            modifier = Modifier.fillMaxWidth(), minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { mostrarTimePicker = true }, modifier = Modifier.weight(1f)) {
                Text("Hora: ${String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)}")
            }
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { mostrarOpcionesFoto = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Foto", modifier = Modifier.padding(end = 4.dp))
                Text(if (fotoUri != null) "Cambiar Foto" else "Evidencia")
            }

            OutlinedButton(
                onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "GPS", modifier = Modifier.padding(end = 4.dp))
                Text(if (ubicacion != null) "Ubicación OK" else "GPS")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (fotoUri != null) {
                AsyncImage(
                    model = fotoUri,
                    contentDescription = "Evidencia Capturada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            if (ubicacion != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, contentDescription = "Mapa", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Text(ubicacion!!, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                val horaFmt = String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)
                onGuardar(titulo, descripcion, categoria, urgente, horaFmt, fotoUri?.toString(), ubicacion)
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

    if (mostrarOpcionesFoto) {
        AlertDialog(
            onDismissRequest = { mostrarOpcionesFoto = false },
            title = { Text("Adjuntar Evidencia") },
            text = {
                Column {
                    TextButton(onClick = {
                        mostrarOpcionesFoto = false
                        try {
                            tempUri = crearUriTemporal(context)
                            tempUri?.let { cameraLauncher.launch(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) { Text("Tomar Foto con Cámara") }

                    TextButton(onClick = {
                        mostrarOpcionesFoto = false
                        galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Elegir de Galería") }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarOpcionesFoto = false }) { Text("Cancelar") }
            }
        )
    }
}
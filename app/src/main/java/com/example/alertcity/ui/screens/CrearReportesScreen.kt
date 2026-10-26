package com.example.alertcity.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.Calendar
import java.util.Locale

fun crearUriTemporal(context: Context): Uri {
    val directorio = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val archivo = File(directorio, "evidencia_${System.currentTimeMillis()}.jpg").apply { createNewFile() }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        archivo
    )
}

fun copiarAAlmacenamientoInterno(context: Context, uriOrigen: Uri): Uri {
    return try {
        val directorio = File(context.filesDir, "reportes_fotos").apply { if (!exists()) mkdirs() }
        val archivoDestino = File(directorio, "foto_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uriOrigen)?.use { input ->
            archivoDestino.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(archivoDestino)
    } catch (e: Exception) {
        e.printStackTrace()
        uriOrigen
    }
}

fun obtenerCoordenadasDispositivo(context: Context): String {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                "${String.format(Locale.US, "%.4f", location.latitude)}° N, ${String.format(Locale.US, "%.4f", location.longitude)}° W"
            } else {
                "20.1387° N, -101.1895° W"
            }
        } else {
            "20.1387° N, -101.1895° W"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "20.1387° N, -101.1895° W"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReportesScreen(onGuardar: (String, String, String, Boolean, String, String?, String?) -> Unit) {
    val context = LocalContext.current

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var urgente by remember { mutableStateOf(false) }

    val categorias = listOf(
        "Baches y Vialidad",
        "Alumbrado Público",
        "Fugas de Agua",
        "Basura y Limpieza",
        "Seguridad y Vandalismo",
        "Parques y Áreas Verdes",
        "Ruido Excesivo",
        "Animales en Abandono",
        "Otro"
    )
    var categoria by remember { mutableStateOf(categorias.first()) }
    var categoriaExpandida by remember { mutableStateOf(false) }

    // Hora actual por defecto
    val calendario = remember { Calendar.getInstance() }
    val timeState = rememberTimePickerState(
        initialHour = calendario.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendario.get(Calendar.MINUTE),
        is24Hour = true
    )
    var mostrarTimePicker by remember { mutableStateOf(false) }

    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarOpcionesFoto by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            fotoUri = copiarAAlmacenamientoInterno(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito && tempUri != null) {
            fotoUri = copiarAAlmacenamientoInterno(context, tempUri!!)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            ubicacion = obtenerCoordenadasDispositivo(context)
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Nuevo Reporte", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Incidencia") },
            placeholder = { Text("Ej. Lámpara rota en parque central") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            placeholder = { Text("Detalla lo que ocurre...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector de Categoría (Dropdown)
        ExposedDropdownMenuBox(
            expanded = categoriaExpandida,
            onExpandedChange = { categoriaExpandida = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = categoria,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría del problema") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpandida) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoriaExpandida,
                onDismissRequest = { categoriaExpandida = false }
            ) {
                categorias.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            categoria = cat
                            categoriaExpandida = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selección de hora (por defecto la actual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { mostrarTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Hora",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Hora del incidente: ${String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = urgente, onCheckedChange = { urgente = it })
            Text("Marcar como urgente", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de acción: Evidencia y GPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Text(if (ubicacion != null) "Ubicación OK" else "Obtener GPS")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Previsualizaciones de foto y ubicación si existen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Mapa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = ubicacion!!,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val horaFmt = String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)
                onGuardar(
                    titulo,
                    descripcion,
                    categoria,
                    urgente,
                    horaFmt,
                    fotoUri?.toString(),
                    ubicacion
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = titulo.isNotBlank() && descripcion.isNotBlank()
        ) {
            Text("Guardar Reporte", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
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

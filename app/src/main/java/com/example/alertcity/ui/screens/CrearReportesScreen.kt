package com.example.alertcity.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import java.io.File
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
fun CrearReportesScreen(
    viewModel: CrearReporteViewModel,
    onReporteGuardado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val uriInterna = copiarAAlmacenamientoInterno(context, uri)
            viewModel.onFotoCapturada(uriInterna.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val tempUriActual = viewModel.uiState.value.tempUri
        if (exito && tempUriActual != null) {
            val uriInterna = copiarAAlmacenamientoInterno(context, Uri.parse(tempUriActual))
            viewModel.onFotoCapturada(uriInterna.toString())
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            val coordenadas = obtenerCoordenadasDispositivo(context)
            viewModel.onUbicacionObtenida(coordenadas)
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
            value = uiState.titulo,
            onValueChange = { viewModel.onTituloChange(it) },
            label = { Text("Incidencia") },
            placeholder = { Text("Ej. Lámpara rota en parque central") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.descripcion,
            onValueChange = { viewModel.onDescripcionChange(it) },
            label = { Text("Descripción") },
            placeholder = { Text("Detalla lo que ocurre...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector de Categoría (Dropdown)
        ExposedDropdownMenuBox(
            expanded = uiState.categoriaExpandida,
            onExpandedChange = { viewModel.onCategoriaExpandidaChange(it) },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.categoria,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría del problema") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.categoriaExpandida) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = uiState.categoriaExpandida,
                onDismissRequest = { viewModel.onCategoriaExpandidaChange(false) }
            ) {
                uiState.categorias.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { viewModel.onCategoriaChange(cat) }
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
                onClick = { viewModel.onMostrarTimePickerChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Hora",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Hora del incidente: ${uiState.horaFormateada}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.urgente,
                onCheckedChange = { viewModel.onUrgenteChange(it) }
            )
            Text("Marcar como urgente", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de acción: Evidencia y GPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.onMostrarOpcionesFotoChange(true) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Foto", modifier = Modifier.padding(end = 4.dp))
                Text(if (uiState.fotoUri != null) "Cambiar Foto" else "Evidencia")
            }

            OutlinedButton(
                onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "GPS", modifier = Modifier.padding(end = 4.dp))
                Text(if (uiState.ubicacion != null) "Ubicación OK" else "Obtener GPS")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Previsualizaciones de foto y ubicación si existen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.fotoUri?.let { fotoActual ->
                AsyncImage(
                    model = fotoActual,
                    contentDescription = "Evidencia Capturada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            uiState.ubicacion?.let { ubicacionActual ->
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
                            text = ubicacionActual,
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
            onClick = { viewModel.guardarReporte(onReporteGuardado) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.formularioValido
        ) {
            Text("Guardar Reporte", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (uiState.mostrarTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = uiState.hora,
            initialMinute = uiState.minuto,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { viewModel.onMostrarTimePickerChange(false) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmarHora(timeState.hour, timeState.minute) }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onMostrarTimePickerChange(false) }
                ) {
                    Text("Cancelar")
                }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (uiState.mostrarOpcionesFoto) {
        AlertDialog(
            onDismissRequest = { viewModel.onMostrarOpcionesFotoChange(false) },
            title = { Text("Adjuntar Evidencia") },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.onMostrarOpcionesFotoChange(false)
                        try {
                            val nuevoTempUri = crearUriTemporal(context)
                            viewModel.onTempUriGenerada(nuevoTempUri.toString())
                            cameraLauncher.launch(nuevoTempUri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) { Text("Tomar Foto con Cámara") }

                    TextButton(onClick = {
                        viewModel.onMostrarOpcionesFotoChange(false)
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Elegir de Galería") }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onMostrarOpcionesFotoChange(false) }) { Text("Cancelar") }
            }
        )
    }
}

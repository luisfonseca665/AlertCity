package com.example.alertcity.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.alertcity.domain.util.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.LONGITUD_CIUDAD_DEFECTO
import com.example.alertcity.ui.components.OsmMapCard
import com.example.alertcity.ui.components.PinMapaUi
import com.example.alertcity.ui.components.colorHexPin
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import java.io.File

private fun crearUriTemporal(context: Context): Uri {
    val directorio = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val archivo = File(directorio, "evidencia_${System.currentTimeMillis()}.jpg").apply { createNewFile() }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        archivo
    )
}

private fun copiarAAlmacenamientoInterno(context: Context, uriOrigen: Uri): Uri {
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

private fun obtenerCoordenadasDispositivo(context: Context): Pair<Double, Double> {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                location.latitude to location.longitude
            } else {
                LATITUD_CIUDAD_DEFECTO to LONGITUD_CIUDAD_DEFECTO
            }
        } else {
            LATITUD_CIUDAD_DEFECTO to LONGITUD_CIUDAD_DEFECTO
        }
    } catch (e: Exception) {
        e.printStackTrace()
        LATITUD_CIUDAD_DEFECTO to LONGITUD_CIUDAD_DEFECTO
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

    LaunchedEffect(Unit) {
        viewModel.sincronizarHoraActualSiNoEsPersonalizada()
    }

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
            viewModel.onUbicacionGpsObtenida(coordenadas.first, coordenadas.second)
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
                Icon(Icons.Default.LocationOn, contentDescription = "Ubicación", modifier = Modifier.padding(end = 4.dp))
                Text(if (uiState.ubicacion != null) "Cambiar Ubicación" else "Ubicación")
            }
        }

        // Previsualización de foto si existe
        uiState.fotoUri?.let { fotoActual ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Evidencia fotográfica",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(
                model = fotoActual,
                contentDescription = "Evidencia Capturada",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mapa interactivo para seleccionar o confirmar ubicación
        Text(
            text = "Ubicación del incidente",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = uiState.ubicacion?.let { "Seleccionada: $it (puedes tocar otro punto en el mapa)" }
                ?: "Toca el punto en el mapa o usa el botón 'Ubicación'",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        MapaSeleccionUbicacionCard(
            latitudSeleccionada = uiState.latitudSeleccionada,
            longitudSeleccionada = uiState.longitudSeleccionada,
            urgente = uiState.urgente,
            ubicacionTexto = uiState.ubicacion,
            latitudCamara = uiState.latitudCamara,
            longitudCamara = uiState.longitudCamara,
            animarMapaTrigger = uiState.animarMapaTrigger,
            onPuntoSeleccionado = viewModel::onMapaClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.guardarReporte(onReporteGuardado) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.formularioValido
        ) {
            Text(
                text = if (uiState.guardando) "Guardando..." else "Guardar Reporte",
                style = MaterialTheme.typography.titleMedium
            )
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

@Composable
private fun MapaSeleccionUbicacionCard(
    latitudSeleccionada: Double?,
    longitudSeleccionada: Double?,
    urgente: Boolean,
    ubicacionTexto: String?,
    latitudCamara: Double,
    longitudCamara: Double,
    animarMapaTrigger: Int,
    onPuntoSeleccionado: (Double, Double) -> Unit
) {
    val pines = remember(latitudSeleccionada, longitudSeleccionada, urgente) {
        if (latitudSeleccionada != null && longitudSeleccionada != null) {
            listOf(
                PinMapaUi(
                    id = "seleccion_actual",
                    latitud = latitudSeleccionada,
                    longitud = longitudSeleccionada,
                    colorHex = colorHexPin(esUrgente = urgente),
                    titulo = "Ubicación del reporte"
                )
            )
        } else {
            emptyList()
        }
    }

    OsmMapCard(
        latitudCamara = latitudCamara,
        longitudCamara = longitudCamara,
        pines = pines,
        zoomInicial = 15.5,
        zoomCamara = 16.5,
        animarTrigger = animarMapaTrigger,
        etiquetaInferior = ubicacionTexto,
        onMapClick = onPuntoSeleccionado
    )
}

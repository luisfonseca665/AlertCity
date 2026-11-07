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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.alertcity.domain.util.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.LONGITUD_CIUDAD_DEFECTO
import com.example.alertcity.ui.components.ImagenPreviewDialog
import com.example.alertcity.ui.components.MapaPreviewDialog
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
    onBack: () -> Unit = {},
    onReporteGuardado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var mostrarPreviewImagen by remember { mutableStateOf(false) }
    var mostrarPreviewMapa by remember { mutableStateOf(false) }

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

    val pinesSeleccion = remember(uiState.latitudSeleccionada, uiState.longitudSeleccionada, uiState.urgente) {
        if (uiState.latitudSeleccionada != null && uiState.longitudSeleccionada != null) {
            listOf(
                PinMapaUi(
                    id = "seleccion_actual",
                    latitud = uiState.latitudSeleccionada!!,
                    longitud = uiState.longitudSeleccionada!!,
                    colorHex = colorHexPin(esUrgente = uiState.urgente),
                    titulo = "Ubicación del reporte"
                )
            )
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar"
                )
            }
            Text(
                text = "Nuevo Reporte",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.titulo,
            onValueChange = { viewModel.onTituloChange(it) },
            label = { Text("Incidencia") },
            placeholder = { Text("Ej. Lámpara rota en parque central") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.descripcion,
            onValueChange = { viewModel.onDescripcionChange(it) },
            label = { Text("Descripción") },
            placeholder = { Text("Detalla lo que ocurre...") },
            shape = RoundedCornerShape(12.dp),
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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

        OutlinedButton(
            onClick = { viewModel.onMostrarTimePickerChange(true) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Hora",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Hora del incidente: ${uiState.horaFormateada}")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            onClick = { viewModel.onUrgenteChange(!uiState.urgente) },
            shape = RoundedCornerShape(12.dp),
            color = if (uiState.urgente) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.urgente,
                    onCheckedChange = { viewModel.onUrgenteChange(it) }
                )
                Text(
                    text = "Marcar como urgente",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (uiState.urgente) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Botones de acción: Evidencia y GPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.onMostrarOpcionesFotoChange(true) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Foto", modifier = Modifier.padding(end = 4.dp))
                Text(if (uiState.fotoUri != null) "Cambiar Foto" else "Evidencia")
            }

            OutlinedButton(
                onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Ubicación", modifier = Modifier.padding(end = 4.dp))
                Text(if (uiState.ubicacion != null) "Cambiar Ubicación" else "Ubicación")
            }
        }

        // Previsualización estética de foto si existe
        uiState.fotoUri?.let { fotoActual ->
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Evidencia fotográfica",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            ElevatedCard(
                onClick = { mostrarPreviewImagen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                ) {
                    AsyncImage(
                        model = fotoActual,
                        contentDescription = "Evidencia Capturada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Previsualizar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mapa interactivo para seleccionar o previsualizar ubicación
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ubicación del incidente",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = { mostrarPreviewMapa = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ampliar mapa",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Text(
            text = uiState.ubicacion?.let { "Seleccionada: $it (puedes tocar otro punto en el mapa)" }
                ?: "Toca el punto en el mapa o usa el botón 'Ubicación'",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OsmMapCard(
            latitudCamara = uiState.latitudCamara,
            longitudCamara = uiState.longitudCamara,
            pines = pinesSeleccion,
            zoomInicial = 15.5,
            zoomCamara = 16.5,
            animarTrigger = uiState.animarMapaTrigger,
            etiquetaInferior = uiState.ubicacion,
            onAmpliarClick = { mostrarPreviewMapa = true },
            onMapClick = viewModel::onMapaClick
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = { viewModel.guardarReporte(onReporteGuardado) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = uiState.formularioValido
        ) {
            Text(
                text = if (uiState.guardando) "Guardando..." else "Guardar Reporte",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (mostrarPreviewImagen && uiState.fotoUri != null) {
        ImagenPreviewDialog(
            imagenUri = uiState.fotoUri!!,
            titulo = uiState.titulo.ifBlank { "Evidencia fotográfica" },
            onDismiss = { mostrarPreviewImagen = false }
        )
    }

    if (mostrarPreviewMapa) {
        MapaPreviewDialog(
            titulo = "Seleccionar ubicación",
            subtitulo = uiState.ubicacion ?: "Toca un punto en el mapa para fijar la ubicación",
            latitudInicial = uiState.latitudSeleccionada ?: uiState.latitudCamara,
            longitudInicial = uiState.longitudSeleccionada ?: uiState.longitudCamara,
            pines = pinesSeleccion,
            zoomInicial = 16.5,
            etiquetaInferior = uiState.ubicacion,
            onMapClick = viewModel::onMapaClick,
            onDismiss = { mostrarPreviewMapa = false }
        )
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

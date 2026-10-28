package com.example.alertcity.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.alertcity.ui.components.aplicarEstiloModerno
import com.example.alertcity.ui.components.configurarOsmdroid
import com.example.alertcity.ui.components.crearPinMapa
import com.example.alertcity.ui.viewmodel.CrearReporteUiState
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import java.io.File
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

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

fun obtenerCoordenadasDispositivo(context: Context): Pair<Double, Double> {
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
                20.1387 to -101.1895
            }
        } else {
            20.1387 to -101.1895
        }
    } catch (e: Exception) {
        e.printStackTrace()
        20.1387 to -101.1895
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
                Icon(Icons.Default.LocationOn, contentDescription = "Ubicación", modifier = Modifier.padding(end = 4.dp))
                Text(if (uiState.ubicacion != null) "Cambiar Ubicación" else "Ubicación")
            }
        }

        // Previsualizacion de foto si existe
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

        // Mapa interactivo para seleccionar o confirmar ubicacion
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
            uiState = uiState,
            onPuntoSeleccionado = { lat, lon -> viewModel.onMapaClick(lat, lon) }
        )

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

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun MapaSeleccionUbicacionCard(
    uiState: CrearReporteUiState,
    onPuntoSeleccionado: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        configurarOsmdroid(context)
        MapView(context).apply {
            aplicarEstiloModerno()
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(uiState.latitudCamara, uiState.longitudCamara))
            setOnTouchListener { vista, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        vista.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        vista.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mapa ->
                    mapa.overlays.clear()

                    val receptorToques = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onPuntoSeleccionado(p.latitude, p.longitude)
                            }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onPuntoSeleccionado(p.latitude, p.longitude)
                            }
                            return true
                        }
                    }
                    mapa.overlays.add(MapEventsOverlay(receptorToques))

                    val latPin = uiState.latitudSeleccionada
                    val lonPin = uiState.longitudSeleccionada
                    if (latPin != null && lonPin != null) {
                        val colorPin = if (uiState.urgente) "#D32F2F" else "#1565C0"
                        val marker = Marker(mapa).apply {
                            position = GeoPoint(latPin, lonPin)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = crearPinMapa(context, colorPin)
                            title = uiState.titulo.ifBlank { "Ubicación del reporte" }
                        }
                        mapa.overlays.add(marker)
                    }

                    val claveCamara = "${uiState.latitudCamara},${uiState.longitudCamara},${uiState.animarMapaTrigger}"
                    if (uiState.animarMapaTrigger > 0 && mapa.tag != claveCamara) {
                        mapa.tag = claveCamara
                        mapa.controller.animateTo(
                            GeoPoint(uiState.latitudCamara, uiState.longitudCamara),
                            16.5,
                            500L
                        )
                    }

                    mapa.invalidate()
                }
            )

            if (uiState.ubicacion != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.ubicacion ?: "",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

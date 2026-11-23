package com.example.alertcity.ui.screens

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.model.SyncStatus
import com.example.alertcity.domain.util.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.LONGITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.parsearCoordenadasUbicacion
import com.example.alertcity.ui.components.aplicarEstiloModerno
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.components.configurarOsmdroid
import com.example.alertcity.ui.components.crearPinMapa
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleReporteScreen(
    reporte: Reporte?,
    puedeCambiarEstado: Boolean = false,
    actualizandoEstado: Boolean = false,
    sincronizando: Boolean = false,
    mensajeAccion: String? = null,
    onLimpiarMensaje: () -> Unit = {},
    onCambiarEstado: (EstadoReporte) -> Unit = {},
    onReintentarSincronizacion: () -> Unit = {},
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var estuvoCargado by remember { mutableStateOf(false) }

    LaunchedEffect(reporte) {
        if (reporte != null) {
            estuvoCargado = true
        }
    }

    LaunchedEffect(mensajeAccion) {
        if (!mensajeAccion.isNullOrBlank()) {
            snackbarHostState.showSnackbar(mensajeAccion)
            onLimpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Reporte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        if (reporte == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (estuvoCargado) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Reporte sincronizado con el servidor.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onBack) {
                            Text("Volver al listado")
                        }
                    }
                } else {
                    Text("Reporte no encontrado")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = reporte.titulo, style = MaterialTheme.typography.headlineMedium)

            if (reporte.syncStatus != SyncStatus.SYNCED) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                                    Icons.Default.ErrorOutline
                                } else {
                                    Icons.Default.CloudQueue
                                },
                                contentDescription = null,
                                tint = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reporte.syncStatus.etiqueta,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                        if (!reporte.syncError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reporte.syncError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onReintentarSincronizacion,
                            enabled = !sincronizando
                        ) {
                            Text(if (sincronizando) "Sincronizando..." else "Reintentar sincronización")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Estatus: ${reporte.estado.etiqueta}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { reporte.estado.progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (reporte.estado == EstadoReporte.RESUELTO) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Control de roles (RBAC): Solo ADMIN u operadores departamentales pueden cambiar el estado
            if (puedeCambiarEstado && reporte.syncStatus == SyncStatus.SYNCED) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Actualizar estado (Operador)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val estadosPermitidos = listOf(
                        EstadoReporte.ENVIADO,
                        EstadoReporte.EN_PROCESO,
                        EstadoReporte.RESUELTO
                    )
                    estadosPermitidos.forEach { estadoOpcion ->
                        val seleccionado = reporte.estado == estadoOpcion
                        FilterChip(
                            selected = seleccionado,
                            enabled = !actualizandoEstado && !seleccionado,
                            onClick = { onCambiarEstado(estadoOpcion) },
                            label = { Text(estadoOpcion.etiqueta) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (actualizandoEstado) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = reporte.categoria,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (reporte.esUrgente) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "URGENTE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Fecha",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reporte.fechaFormateada,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Hora",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reporte.hora,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Descripción", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = reporte.descripcion, style = MaterialTheme.typography.bodyLarge)

            if (reporte.fotoUri != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(text = "Evidencia fotográfica", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = reporte.fotoUri,
                    contentDescription = "Evidencia",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "Ubicación", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            MapaDetalleReporteCard(reporte = reporte)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun MapaDetalleReporteCard(reporte: Reporte) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val coordenadasParseadas = parsearCoordenadasUbicacion(reporte.ubicacion)
    val latitud = coordenadasParseadas?.first ?: LATITUD_CIUDAD_DEFECTO
    val longitud = coordenadasParseadas?.second ?: LONGITUD_CIUDAD_DEFECTO
    val etiquetaUbicacion = reporte.ubicacion ?: "Ubicación aproximada"

    val mapView = remember(reporte.idReporte) {
        configurarOsmdroid(context)
        MapView(context).apply {
            aplicarEstiloModerno()
            controller.setZoom(16.5)
            controller.setCenter(GeoPoint(latitud, longitud))
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
                    val marker = Marker(mapa).apply {
                        position = GeoPoint(latitud, longitud)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = crearPinMapa(context, colorHexPorReporte(reporte))
                        title = reporte.titulo
                        subDescription = "${reporte.categoria} • ${reporte.fechaHoraFormateada} • ${reporte.estado.etiqueta}"
                    }
                    mapa.overlays.add(marker)
                    marker.showInfoWindow()
                    mapa.invalidate()
                }
            )

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
                        text = etiquetaUbicacion,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

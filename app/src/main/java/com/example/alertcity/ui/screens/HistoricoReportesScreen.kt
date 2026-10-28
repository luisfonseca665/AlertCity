package com.example.alertcity.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.ui.components.aplicarEstiloModerno
import com.example.alertcity.ui.components.configurarOsmdroid
import com.example.alertcity.ui.components.crearPinMapa
import com.example.alertcity.ui.viewmodel.HistoricoReportesUiState
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private fun crearIconoPin(context: Context, reporte: Reporte): Drawable? {
    val colorHex = when {
        reporte.estado == EstadoReporte.RESUELTO -> "#2E7D32"
        reporte.esUrgente -> "#D32F2F"
        else -> "#1565C0"
    }
    return crearPinMapa(context, colorHex)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoReportesScreen(
    viewModel: HistoricoReportesViewModel,
    onReporteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cabecera compacta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Histórico",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Historial de Reportes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.reportes.size} incidencias registradas en el mapa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Leyenda de colores de pines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeyendaItem(color = Color(0xFFD32F2F), texto = "Urgente")
            LeyendaItem(color = Color(0xFF1565C0), texto = "Activo")
            LeyendaItem(color = Color(0xFF2E7D32), texto = "Resuelto")
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mapa fijo en la parte superior
        MapaIncidenciasCard(
            uiState = uiState,
            onPinClick = { id -> viewModel.seleccionarReporteEnMapa(id) },
            onCentrarMapa = { viewModel.centrarVistaGeneral() },
            onCerrarSeleccion = { viewModel.limpiarSeleccionMapa() },
            onVerDetalle = { id -> onReporteClick(id) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Lista de reportes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Lista de reportes en la parte inferior
        if (uiState.reportes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay reportes disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(uiState.reportes, key = { it.idReporte }) { reporte ->
                    val estaSeleccionado = uiState.reporteSeleccionado?.idReporte == reporte.idReporte

                    OutlinedCard(
                        onClick = { onReporteClick(reporte.idReporte) },
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth(),
                        border = BorderStroke(
                            width = if (estaSeleccionado) 2.dp else 1.dp,
                            color = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (estaSeleccionado) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = reporte.titulo,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (reporte.esUrgente) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "URGENTE",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${reporte.categoria} • ${reporte.hora}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Estado: ${reporte.estado.etiqueta}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (reporte.estado.progreso == 1.0f) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )
                            }

                            IconButton(
                                onClick = { viewModel.seleccionarReporteEnMapa(reporte.idReporte) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Ubicar en el mapa",
                                    tint = if (estaSeleccionado) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeyendaItem(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = texto, style = MaterialTheme.typography.labelSmall)
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun MapaIncidenciasCard(
    uiState: HistoricoReportesUiState,
    onPinClick: (String) -> Unit,
    onCentrarMapa: () -> Unit,
    onCerrarSeleccion: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        configurarOsmdroid(context)
        MapView(context).apply {
            aplicarEstiloModerno()
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(uiState.latitudCentro, uiState.longitudCentro))
            setOnTouchListener { vista, _ ->
                vista.parent?.requestDisallowInterceptTouchEvent(true)
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
            .height(250.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mapa ->
                    mapa.overlays.clear()

                    uiState.marcadores.forEach { marcador ->
                        val marker = Marker(mapa).apply {
                            position = GeoPoint(marcador.latitud, marcador.longitud)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = crearIconoPin(context, marcador.reporte)
                            title = marcador.reporte.titulo
                            subDescription = "${marcador.reporte.categoria} • ${marcador.reporte.estado.etiqueta}" +
                                if (!marcador.tieneUbicacionExacta) " (Ubicación aprox.)" else ""
                            setOnMarkerClickListener { m, _ ->
                                m.showInfoWindow()
                                onPinClick(marcador.reporte.idReporte)
                                true
                            }
                        }
                        mapa.overlays.add(marker)

                        if (uiState.reporteSeleccionado?.idReporte == marcador.reporte.idReporte) {
                            marker.showInfoWindow()
                        }
                    }

                    val claveEnfoque = "${uiState.latitudCentro},${uiState.longitudCentro},${uiState.zoomMapa},${uiState.enfocarTrigger}"
                    if (mapa.tag != claveEnfoque) {
                        mapa.tag = claveEnfoque
                        mapa.controller.animateTo(
                            GeoPoint(uiState.latitudCentro, uiState.longitudCentro),
                            uiState.zoomMapa,
                            500L
                        )
                    }

                    mapa.invalidate()
                }
            )

            // Boton flotante para recentrar el mapa
            SmallFloatingActionButton(
                onClick = onCentrarMapa,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Centrar mapa"
                )
            }

            // Tarjeta flotante cuando se selecciona un pin en el mapa
            uiState.reporteSeleccionado?.let { seleccionado ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = seleccionado.titulo,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${seleccionado.categoria} • ${seleccionado.estado.etiqueta}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onVerDetalle(seleccionado.idReporte) }) {
                            Text("Ver detalle")
                        }
                        IconButton(
                            onClick = onCerrarSeleccion,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

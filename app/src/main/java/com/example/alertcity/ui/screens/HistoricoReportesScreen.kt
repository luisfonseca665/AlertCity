package com.example.alertcity.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alertcity.R
import com.example.alertcity.data.location.LocationHelper
import com.example.alertcity.ui.components.COLOR_PIN_ACTIVO
import com.example.alertcity.ui.components.COLOR_PIN_RESUELTO
import com.example.alertcity.ui.components.COLOR_PIN_URGENTE
import com.example.alertcity.ui.components.MapaPreviewDialog
import com.example.alertcity.ui.components.OsmMapCard
import com.example.alertcity.ui.components.PinMapaUi
import com.example.alertcity.ui.components.ReporteItemCard
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.components.esPantallaGrande
import com.example.alertcity.ui.viewmodel.HistoricoReportesUiState
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoReportesScreen(
    viewModel: HistoricoReportesViewModel,
    onReporteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pantallaGrande = esPantallaGrande()

    val nearbyLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        coroutineScope.launch {
            val (lat, lng) = LocationHelper.obtenerCoordenadas(context)
            viewModel.cargarReportesCercanos(lat, lng, radiusKm = 3.0)
        }
    }

    val seleccionadoId = uiState.reporteSeleccionado?.idReporte
    val pines = remember(uiState.marcadores, seleccionadoId) {
        uiState.marcadores.map { marcador ->
            PinMapaUi(
                id = marcador.reporte.idReporte,
                latitud = marcador.latitud,
                longitud = marcador.longitud,
                colorHex = colorHexPorReporte(marcador.reporte),
                titulo = marcador.reporte.titulo,
                descripcion = "${marcador.reporte.categoria} • ${marcador.reporte.fechaHoraFormateada} • ${marcador.reporte.estado.etiqueta}" +
                    if (!marcador.tieneUbicacionExacta) " (Ubicación aprox.)" else "",
                mostrarInfoWindow = seleccionadoId == marcador.reporte.idReporte
            )
        }
    }

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
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(id = R.string.nav_history),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.modoCercanos) {
                        stringResource(id = R.string.history_nearby_title)
                    } else {
                        stringResource(id = R.string.history_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.history_subtitle, uiState.reportes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selector de modo: Todos vs Cercanos (PostGIS /api/v1/reports/nearby o Haversine offline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !uiState.modoCercanos,
                onClick = { viewModel.mostrarTodos() },
                label = { Text(stringResource(id = R.string.filter_all)) }
            )
            FilterChip(
                selected = uiState.modoCercanos,
                onClick = {
                    if (LocationHelper.tienePermisoUbicacion(context)) {
                        coroutineScope.launch {
                            val (lat, lng) = LocationHelper.obtenerCoordenadas(context)
                            viewModel.cargarReportesCercanos(lat, lng, radiusKm = 3.0)
                        }
                    } else {
                        nearbyLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(id = R.string.history_filter_nearby)) }
            )
        }

        uiState.mensajeError?.let { error ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
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
            LeyendaItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_URGENTE)), texto = stringResource(id = R.string.legend_urgent))
            LeyendaItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_ACTIVO)), texto = stringResource(id = R.string.legend_active))
            LeyendaItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_RESUELTO)), texto = stringResource(id = R.string.legend_resolved))
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (pantallaGrande) {
            // Diseño adaptativo de 2 paneles lado a lado para pantallas grandes / tablets
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(bottom = 8.dp)
                ) {
                    MapaIncidenciasCard(
                        uiState = uiState,
                        pines = pines,
                        ocuparTodoElAlto = true,
                        modifier = Modifier.fillMaxSize(),
                        onPinClick = viewModel::seleccionarReporteEnMapa,
                        onCentrarMapa = viewModel::centrarVistaGeneral,
                        onAmpliarMapa = { viewModel.onMostrarPreviewMapaChange(true) },
                        onCerrarSeleccion = viewModel::limpiarSeleccionMapa,
                        onVerDetalle = onReporteClick
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = stringResource(id = R.string.history_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    ListadoReportesHistorico(
                        uiState = uiState,
                        onRefresh = viewModel::refrescar,
                        onReporteClick = onReporteClick,
                        onSeleccionarEnMapa = viewModel::seleccionarReporteEnMapa,
                        contentPadding = PaddingValues(vertical = 6.dp)
                    )
                }
            }
        } else {
            // Diseño compacto vertical para teléfonos
            MapaIncidenciasCard(
                uiState = uiState,
                pines = pines,
                ocuparTodoElAlto = false,
                modifier = Modifier.padding(horizontal = 16.dp),
                onPinClick = viewModel::seleccionarReporteEnMapa,
                onCentrarMapa = viewModel::centrarVistaGeneral,
                onAmpliarMapa = { viewModel.onMostrarPreviewMapaChange(true) },
                onCerrarSeleccion = viewModel::limpiarSeleccionMapa,
                onVerDetalle = onReporteClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.history_list_header),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            ListadoReportesHistorico(
                uiState = uiState,
                onRefresh = viewModel::refrescar,
                onReporteClick = onReporteClick,
                onSeleccionarEnMapa = viewModel::seleccionarReporteEnMapa,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    if (uiState.mostrarPreviewMapa) {
        MapaPreviewDialog(
            titulo = stringResource(id = R.string.history_map_dialog_title),
            subtitulo = stringResource(id = R.string.history_map_dialog_subtitle, uiState.reportes.size),
            latitudInicial = uiState.latitudCentro,
            longitudInicial = uiState.longitudCentro,
            pines = pines,
            zoomInicial = uiState.zoomMapa,
            onDismiss = { viewModel.onMostrarPreviewMapaChange(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListadoReportesHistorico(
    uiState: HistoricoReportesUiState,
    onRefresh: () -> Unit,
    onReporteClick: (String) -> Unit,
    onSeleccionarEnMapa: (String) -> Unit,
    contentPadding: PaddingValues
) {
    PullToRefreshBox(
        isRefreshing = uiState.cargando,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            if (uiState.reportes.isEmpty() && !uiState.cargando) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.history_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.reportes, key = { it.idReporte }) { reporte ->
                    val estaSeleccionado = uiState.reporteSeleccionado?.idReporte == reporte.idReporte

                    ReporteItemCard(
                        reporte = reporte,
                        onClick = { onReporteClick(reporte.idReporte) },
                        modifier = Modifier.padding(bottom = 10.dp),
                        seleccionado = estaSeleccionado,
                        onUbicarEnMapaClick = { onSeleccionarEnMapa(reporte.idReporte) }
                    )
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

@Composable
private fun MapaIncidenciasCard(
    uiState: HistoricoReportesUiState,
    pines: List<PinMapaUi>,
    ocuparTodoElAlto: Boolean,
    modifier: Modifier = Modifier,
    onPinClick: (String) -> Unit,
    onCentrarMapa: () -> Unit,
    onAmpliarMapa: () -> Unit,
    onCerrarSeleccion: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    OsmMapCard(
        latitudCamara = uiState.latitudCentro,
        longitudCamara = uiState.longitudCentro,
        pines = pines,
        modifier = modifier,
        zoomInicial = 15.0,
        zoomCamara = uiState.zoomMapa,
        animarTrigger = uiState.enfocarTrigger,
        alto = if (ocuparTodoElAlto) null else 240.dp,
        ocuparTodoElAlto = ocuparTodoElAlto,
        radioEsquina = 16.dp,
        elevacion = 4.dp,
        onAmpliarClick = onAmpliarMapa,
        alineacionBotonAmpliar = Alignment.TopStart,
        onPinClick = onPinClick
    ) {
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
                contentDescription = null
            )
        }

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
                            text = "${seleccionado.categoria} • ${seleccionado.fechaHoraFormateada}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { onVerDetalle(seleccionado.idReporte) }) {
                        Text(stringResource(id = R.string.action_view_detail))
                    }
                    IconButton(
                        onClick = onCerrarSeleccion,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

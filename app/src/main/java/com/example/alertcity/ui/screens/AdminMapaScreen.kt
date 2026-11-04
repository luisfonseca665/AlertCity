package com.example.alertcity.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.ui.components.AdminTopBar
import com.example.alertcity.ui.components.COLOR_PIN_ACTIVO
import com.example.alertcity.ui.components.COLOR_PIN_RESUELTO
import com.example.alertcity.ui.components.COLOR_PIN_URGENTE
import com.example.alertcity.ui.components.OsmMapCard
import com.example.alertcity.ui.components.PinMapaUi
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.viewmodel.AdminReportesViewModel

@Composable
fun AdminMapaScreen(
    viewModel: AdminReportesViewModel,
    onGestionarReporte: (String) -> Unit,
    onCambiarModoCiudadano: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val seleccionadoId = uiState.reporteSeleccionadoMapa?.idReporte

    val pines = remember(uiState.marcadoresMapa, seleccionadoId) {
        uiState.marcadoresMapa.map { marcador ->
            PinMapaUi(
                id = marcador.reporte.idReporte,
                latitud = marcador.latitud,
                longitud = marcador.longitud,
                colorHex = colorHexPorReporte(marcador.reporte),
                titulo = marcador.reporte.titulo,
                descripcion = "${marcador.reporte.categoria} • ${marcador.reporte.estado.etiqueta}" +
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
        AdminTopBar(
            titulo = "Mapa Operativo",
            subtitulo = "${uiState.marcadoresMapa.size} de ${uiState.totalReportes} incidencias en vista",
            onCambiarModoCiudadano = onCambiarModoCiudadano,
            onRefrescar = viewModel::refrescar
        )

        // Filtros del mapa de monitoreo
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.mapaFiltroEstado == null && !uiState.mapaSoloUrgentes,
                    onClick = {
                        viewModel.onMapaFiltroEstadoChange(null)
                        viewModel.onMapaSoloUrgentesChange(false)
                    },
                    label = { Text("Todos") }
                )
            }
            item {
                FilterChip(
                    selected = uiState.mapaSoloUrgentes,
                    onClick = { viewModel.onMapaSoloUrgentesChange(!uiState.mapaSoloUrgentes) },
                    label = { Text("Solo Urgentes") }
                )
            }
            items(EstadoReporte.entries) { estado ->
                FilterChip(
                    selected = uiState.mapaFiltroEstado == estado,
                    onClick = { viewModel.onMapaFiltroEstadoChange(estado) },
                    label = { Text(estado.etiqueta) }
                )
            }
        }

        // Leyenda de pines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeyendaMapaAdminItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_URGENTE)), texto = "Urgente")
            LeyendaMapaAdminItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_ACTIVO)), texto = "Activo")
            LeyendaMapaAdminItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_RESUELTO)), texto = "Resuelto")
        }

        OsmMapCard(
            latitudCamara = uiState.latitudCentro,
            longitudCamara = uiState.longitudCentro,
            pines = pines,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            zoomInicial = 15.0,
            zoomCamara = uiState.zoomMapa,
            animarTrigger = uiState.enfocarTrigger,
            alto = null,
            radioEsquina = 16.dp,
            elevacion = 4.dp,
            onPinClick = viewModel::seleccionarReporteEnMapa
        ) {
            SmallFloatingActionButton(
                onClick = viewModel::centrarVistaGeneralMapa,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Centrar vista general"
                )
            }

            uiState.reporteSeleccionadoMapa?.let { seleccionado ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = seleccionado.titulo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${seleccionado.categoria} • ${seleccionado.estado.etiqueta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = viewModel::limpiarSeleccionMapa,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar selección",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (seleccionado.estado != EstadoReporte.RESUELTO) {
                                TextButton(onClick = { viewModel.avanzarEstadoReporte(seleccionado) }) {
                                    Text("Avanzar estatus")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            FilledTonalButton(
                                onClick = { onGestionarReporte(seleccionado.idReporte) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Gestionar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeyendaMapaAdminItem(color: Color, texto: String) {
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

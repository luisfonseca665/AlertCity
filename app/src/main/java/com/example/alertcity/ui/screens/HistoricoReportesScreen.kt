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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
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
import com.example.alertcity.ui.components.COLOR_PIN_ACTIVO
import com.example.alertcity.ui.components.COLOR_PIN_RESUELTO
import com.example.alertcity.ui.components.COLOR_PIN_URGENTE
import com.example.alertcity.ui.components.OsmMapCard
import com.example.alertcity.ui.components.PinMapaUi
import com.example.alertcity.ui.components.ReporteItemCard
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.viewmodel.HistoricoReportesUiState
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel

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

        // Leyenda de colores de pines sincronizada con OsmMapHelper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeyendaItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_URGENTE)), texto = "Urgente")
            LeyendaItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_ACTIVO)), texto = "Activo")
            LeyendaItem(color = Color(android.graphics.Color.parseColor(COLOR_PIN_RESUELTO)), texto = "Resuelto")
        }

        Spacer(modifier = Modifier.height(6.dp))

        MapaIncidenciasCard(
            uiState = uiState,
            onPinClick = viewModel::seleccionarReporteEnMapa,
            onCentrarMapa = viewModel::centrarVistaGeneral,
            onCerrarSeleccion = viewModel::limpiarSeleccionMapa,
            onVerDetalle = onReporteClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Lista de reportes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

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

                    ReporteItemCard(
                        reporte = reporte,
                        onClick = { onReporteClick(reporte.idReporte) },
                        modifier = Modifier.padding(bottom = 10.dp),
                        seleccionado = estaSeleccionado,
                        onUbicarEnMapaClick = { viewModel.seleccionarReporteEnMapa(reporte.idReporte) }
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
    onPinClick: (String) -> Unit,
    onCentrarMapa: () -> Unit,
    onCerrarSeleccion: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
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

    OsmMapCard(
        latitudCamara = uiState.latitudCentro,
        longitudCamara = uiState.longitudCentro,
        pines = pines,
        modifier = Modifier.padding(horizontal = 16.dp),
        zoomInicial = 15.0,
        zoomCamara = uiState.zoomMapa,
        animarTrigger = uiState.enfocarTrigger,
        alto = 250.dp,
        radioEsquina = 16.dp,
        elevacion = 4.dp,
        onPinClick = onPinClick
    ) {
        // Botón flotante para recentrar el mapa
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
                            text = "${seleccionado.categoria} • ${seleccionado.fechaHoraFormateada}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.ui.components.AdminTopBar
import com.example.alertcity.ui.components.ReporteItemCard
import com.example.alertcity.ui.viewmodel.AdminReportesViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: AdminReportesViewModel,
    onReporteClick: (String) -> Unit,
    onCambiarModoCiudadano: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.cargando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AdminTopBar(
            titulo = "Panel Admin",
            subtitulo = "Gestión municipal • Uriangato, Gto.",
            onCambiarModoCiudadano = onCambiarModoCiudadano,
            onRefrescar = viewModel::refrescar
        )

        // Resumen ejecutivo por estatus
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricaEstatusItem(
                        valor = uiState.totalEnviados,
                        etiqueta = "Enviados",
                        color = MaterialTheme.colorScheme.primary
                    )
                    VerticalDivider(modifier = Modifier.height(34.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalEnRevision,
                        etiqueta = "Revisión",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    VerticalDivider(modifier = Modifier.height(34.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalEnProceso,
                        etiqueta = "Atendiendo",
                        color = Color(0xFFEF6C00)
                    )
                    VerticalDivider(modifier = Modifier.height(34.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalResueltos,
                        etiqueta = "Resueltos",
                        color = Color(0xFF2E7D32)
                    )
                }

                if (uiState.totalUrgentesActivos > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = { viewModel.onSoloUrgentesChange(!uiState.soloUrgentes) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.totalUrgentesActivos} reportes urgentes pendientes de resolver",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Buscador
        OutlinedTextField(
            value = uiState.busqueda,
            onValueChange = viewModel::onBusquedaChange,
            placeholder = { Text("Buscar por título, categoría o detalle...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (uiState.busqueda.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onBusquedaChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Filtros rápidos por estatus y urgencia
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.filtroEstado == null && !uiState.soloUrgentes,
                    onClick = viewModel::limpiarFiltrosDashboard,
                    label = { Text("Todos (${uiState.totalReportes})") }
                )
            }
            item {
                FilterChip(
                    selected = uiState.soloUrgentes,
                    onClick = { viewModel.onSoloUrgentesChange(!uiState.soloUrgentes) },
                    label = { Text("Urgentes") }
                )
            }
            items(EstadoReporte.entries) { estado ->
                FilterChip(
                    selected = uiState.filtroEstado == estado,
                    onClick = { viewModel.onFiltroEstadoChange(estado) },
                    label = { Text(estado.etiqueta) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bandeja de atención (${uiState.reportesFiltrados.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (uiState.filtroEstado != null || uiState.soloUrgentes || uiState.busqueda.isNotBlank()) {
                TextButton(onClick = viewModel::limpiarFiltrosDashboard) {
                    Text("Limpiar filtros", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (uiState.reportesFiltrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay reportes que coincidan con el filtro actual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(uiState.reportesFiltrados, key = { it.idReporte }) { reporte ->
                    ReporteItemCard(
                        reporte = reporte,
                        onClick = { onReporteClick(reporte.idReporte) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricaEstatusItem(
    valor: Int,
    etiqueta: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$valor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

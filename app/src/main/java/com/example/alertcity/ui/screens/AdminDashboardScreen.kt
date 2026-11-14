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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

@OptIn(ExperimentalMaterial3Api::class)
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
        AdminTopBar(
            titulo = "Panel Admin",
            subtitulo = "Gestión municipal • Uriangato, Gto.",
            onCambiarModoCiudadano = onCambiarModoCiudadano
        )

        // Resumen ejecutivo por estatus (estilo develop)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)) {
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
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalEnRevision,
                        etiqueta = "Revisión",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalEnProceso,
                        etiqueta = "Atendiendo",
                        color = Color(0xFFEF6C00)
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalResueltos,
                        etiqueta = "Resueltos",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (uiState.totalUrgentesActivos > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = { viewModel.onSoloUrgentesChange(!uiState.soloUrgentes) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.soloUrgentes) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.totalUrgentesActivos} reportes urgentes pendientes de resolver",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (uiState.soloUrgentes) "Mostrando" else "Filtrar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
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
            val todosSeleccionado = uiState.filtroEstado == null && !uiState.soloUrgentes
            item {
                FilterChip(
                    selected = todosSeleccionado,
                    onClick = viewModel::limpiarFiltrosDashboard,
                    leadingIcon = if (todosSeleccionado) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = { Text("Todos (${uiState.totalReportes})") }
                )
            }
            item {
                FilterChip(
                    selected = uiState.soloUrgentes,
                    onClick = { viewModel.onSoloUrgentesChange(!uiState.soloUrgentes) },
                    leadingIcon = if (uiState.soloUrgentes) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = { Text("Urgentes") }
                )
            }
            items(EstadoReporte.entries) { estado ->
                val seleccionado = uiState.filtroEstado == estado
                FilterChip(
                    selected = seleccionado,
                    onClick = { viewModel.onFiltroEstadoChange(estado) },
                    leadingIcon = if (seleccionado) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = { Text(estado.etiqueta) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bandeja de atención (${uiState.reportesFiltrados.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (uiState.filtroEstado != null || uiState.soloUrgentes || uiState.busqueda.isNotBlank()) {
                TextButton(onClick = viewModel::limpiarFiltrosDashboard) {
                    Text("Limpiar filtros", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.cargando,
            onRefresh = { viewModel.refrescar() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (uiState.reportesFiltrados.isEmpty() && !uiState.cargando) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay reportes que coincidan con el filtro actual.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

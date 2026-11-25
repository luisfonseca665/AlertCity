package com.example.alertcity.ui.screens

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alertcity.R
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.ui.components.AdminTopBar
import com.example.alertcity.ui.components.AlertCityContentType
import com.example.alertcity.ui.components.ReporteItemCard
import com.example.alertcity.ui.viewmodel.AdminDetalleReporteViewModel
import com.example.alertcity.ui.viewmodel.AdminReportesUiState
import com.example.alertcity.ui.viewmodel.AdminReportesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminReportesViewModel,
    contentType: AlertCityContentType = AlertCityContentType.LIST_ONLY,
    onReporteClick: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (contentType == AlertCityContentType.LIST_AND_DETAIL) {
        // Patrón oficial del codelab: Lista y Detalle lado a lado en pantallas grandes (Expanded)
        val adminDetalleViewModel: AdminDetalleReporteViewModel = viewModel(
            factory = AdminDetalleReporteViewModel.Factory
        )
        val seleccionadoId = uiState.reporteSeleccionadoDetalle?.idReporte
        LaunchedEffect(seleccionadoId) {
            adminDetalleViewModel.cargarReporte(seleccionadoId)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                BandejaReportesAdminContenido(
                    uiState = uiState,
                    viewModel = viewModel,
                    seleccionadoDetalleId = seleccionadoId,
                    onReporteClick = { id -> viewModel.seleccionarReporteDetalle(id) },
                    onCerrarSesion = onCerrarSesion
                )
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight())

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                if (seleccionadoId != null) {
                    AdminDetalleReporteScreen(
                        viewModel = adminDetalleViewModel,
                        onBack = {},
                        mostrarNavegacionAtras = false
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_filtered_reports),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        BandejaReportesAdminContenido(
            uiState = uiState,
            viewModel = viewModel,
            seleccionadoDetalleId = null,
            onReporteClick = onReporteClick,
            onCerrarSesion = onCerrarSesion
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BandejaReportesAdminContenido(
    uiState: AdminReportesUiState,
    viewModel: AdminReportesViewModel,
    seleccionadoDetalleId: String?,
    onReporteClick: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AdminTopBar(
            titulo = uiState.tituloPanel,
            subtitulo = uiState.subtituloPanel,
            onCerrarSesion = onCerrarSesion
        )

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
                        etiqueta = stringResource(id = R.string.metric_sent),
                        color = MaterialTheme.colorScheme.primary
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalEnProceso,
                        etiqueta = stringResource(id = R.string.metric_in_progress),
                        color = Color(0xFFEF6C00)
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    MetricaEstatusItem(
                        valor = uiState.totalResueltos,
                        etiqueta = stringResource(id = R.string.metric_resolved),
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
                                text = stringResource(id = R.string.admin_urgent_pending_banner, uiState.totalUrgentesActivos),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (uiState.soloUrgentes) {
                                    stringResource(id = R.string.admin_showing)
                                } else {
                                    stringResource(id = R.string.admin_filter_action)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = uiState.busqueda,
            onValueChange = viewModel::onBusquedaChange,
            placeholder = { Text(stringResource(id = R.string.admin_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.busqueda.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onBusquedaChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

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
                    label = { Text(stringResource(id = R.string.filter_all_count, uiState.totalReportes)) }
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
                    label = { Text(stringResource(id = R.string.filter_urgent)) }
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
                text = stringResource(id = R.string.admin_inbox_header, uiState.reportesFiltrados.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (uiState.filtroEstado != null || uiState.soloUrgentes || uiState.busqueda.isNotBlank()) {
                TextButton(onClick = viewModel::limpiarFiltrosDashboard) {
                    Text(stringResource(id = R.string.filter_clear), style = MaterialTheme.typography.labelMedium)
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
                                text = stringResource(id = R.string.no_filtered_reports),
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
                            seleccionado = seleccionadoDetalleId == reporte.idReporte,
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

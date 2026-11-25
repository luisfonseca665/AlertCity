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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alertcity.R
import com.example.alertcity.data.local.UserSession
import com.example.alertcity.ui.components.AlertCityContentType
import com.example.alertcity.ui.components.ReporteItemCard
import com.example.alertcity.ui.viewmodel.DetalleReporteViewModel
import com.example.alertcity.ui.viewmodel.ListaReportesUiState
import com.example.alertcity.ui.viewmodel.ReporteViewModel

private val FILTROS_CATEGORIA = listOf(
    "VIALIDAD" to "Vialidad",
    "ALUMBRADO" to "Alumbrado",
    "AGUA" to "Agua",
    "LIMPIEZA" to "Limpieza",
    "SEGURIDAD" to "Seguridad",
    "AREAS_VERDES" to "Áreas Verdes",
    "ANIMALES" to "Animales"
)

private val FILTROS_ESTADO = listOf(
    "ENVIADO" to "Enviado",
    "EN_PROCESO" to "Atendiendo",
    "RESUELTO" to "Resuelto"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaReportesScreen(
    viewModel: ReporteViewModel,
    session: UserSession? = null,
    contentType: AlertCityContentType = AlertCityContentType.LIST_ONLY,
    onCerrarSesion: (() -> Unit)? = null,
    onReporteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(session?.userId) {
        if (session != null) {
            viewModel.refrescar()
        }
    }

    if (contentType == AlertCityContentType.LIST_AND_DETAIL) {
        // Patrón oficial del codelab: Lista y Detalle lado a lado en pantallas grandes (Expanded)
        val detalleViewModel: DetalleReporteViewModel = viewModel(factory = DetalleReporteViewModel.Factory)
        val seleccionadoId = uiState.reporteSeleccionadoDetalle?.idReporte
        LaunchedEffect(seleccionadoId) {
            detalleViewModel.cargarReporte(seleccionadoId)
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
                ListaReportesContenido(
                    uiState = uiState,
                    session = session,
                    viewModel = viewModel,
                    seleccionadoDetalleId = seleccionadoId,
                    onCerrarSesion = onCerrarSesion,
                    onReporteClick = { id -> viewModel.seleccionarReporteDetalle(id) }
                )
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight())

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                if (seleccionadoId != null) {
                    DetalleReporteScreen(
                        viewModel = detalleViewModel,
                        onBack = {},
                        mostrarTopBar = false
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_reports),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        ListaReportesContenido(
            uiState = uiState,
            session = session,
            viewModel = viewModel,
            seleccionadoDetalleId = null,
            onCerrarSesion = onCerrarSesion,
            onReporteClick = onReporteClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaReportesContenido(
    uiState: ListaReportesUiState,
    session: UserSession?,
    viewModel: ReporteViewModel,
    seleccionadoDetalleId: String?,
    onCerrarSesion: (() -> Unit)?,
    onReporteClick: (String) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = uiState.cargando || uiState.sincronizando,
        onRefresh = { viewModel.refrescar() },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session?.firstName?.takeIf { it.isNotBlank() }?.let {
                                stringResource(id = R.string.greeting, it)
                            } ?: stringResource(id = R.string.greeting_default),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = session?.rolLegible ?: stringResource(id = R.string.location),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onCerrarSesion != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onCerrarSesion) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(id = R.string.action_logout),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
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
                    Row(
                        modifier = Modifier
                            .padding(vertical = 18.dp, horizontal = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.misActivosCount ?: uiState.totalReportes}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (uiState.misActivosCount != null) {
                                    stringResource(id = R.string.metric_my_active)
                                } else {
                                    stringResource(id = R.string.metric_reports)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.misResueltosCount ?: uiState.reportes.count { it.estado.progreso == 1.0f }}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = stringResource(id = R.string.metric_resolved),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.totalUrgentes}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(id = R.string.metric_urgent),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!uiState.isOnline || uiState.pendingSyncCount > 0) {
                item {
                    Surface(
                        color = if (!uiState.isOnline) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (!uiState.isOnline) Icons.Default.CloudOff else Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (!uiState.isOnline) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !uiState.isOnline && uiState.pendingSyncCount > 0 ->
                                        stringResource(id = R.string.offline_pending_banner, uiState.pendingSyncCount)
                                    !uiState.isOnline ->
                                        stringResource(id = R.string.offline_local_banner)
                                    else ->
                                        stringResource(id = R.string.online_pending_banner, uiState.pendingSyncCount)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!uiState.isOnline) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.isOnline && uiState.pendingSyncCount > 0) {
                                TextButton(
                                    onClick = { viewModel.sincronizarPendientes() },
                                    enabled = !uiState.sincronizando
                                ) {
                                    Text(
                                        if (uiState.sincronizando) {
                                            stringResource(id = R.string.sync_sending)
                                        } else {
                                            stringResource(id = R.string.sync_action)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = !uiState.tieneFiltrosActivos,
                            onClick = { viewModel.limpiarFiltros() },
                            label = { Text(stringResource(id = R.string.filter_all)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filtroSoloUrgentes,
                            onClick = { viewModel.onFiltroUrgentesChange(!uiState.filtroSoloUrgentes) },
                            label = { Text(stringResource(id = R.string.filter_urgent)) }
                        )
                    }
                    items(FILTROS_ESTADO) { (valor, etiqueta) ->
                        FilterChip(
                            selected = uiState.filtroEstado == valor,
                            onClick = {
                                viewModel.onFiltroEstadoChange(if (uiState.filtroEstado == valor) null else valor)
                            },
                            label = { Text(etiqueta) }
                        )
                    }
                    items(FILTROS_CATEGORIA) { (valor, etiqueta) ->
                        FilterChip(
                            selected = uiState.filtroCategoria == valor,
                            onClick = {
                                viewModel.onFiltroCategoriaChange(if (uiState.filtroCategoria == valor) null else valor)
                            },
                            label = { Text(etiqueta) }
                        )
                    }
                }
            }

            uiState.mensajeError?.let { error ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            val reportesMostrados = uiState.listaVisible

            item {
                Text(
                    text = if (uiState.tieneFiltrosActivos) {
                        stringResource(id = R.string.filtered_reports, reportesMostrados.size)
                    } else {
                        stringResource(id = R.string.my_reports)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)
                )
            }

            if (reportesMostrados.isEmpty() && !uiState.cargando) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.tieneFiltrosActivos) {
                                stringResource(id = R.string.no_filtered_reports)
                            } else {
                                stringResource(id = R.string.no_reports)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(reportesMostrados, key = { it.idReporte }) { reporte ->
                    ReporteItemCard(
                        reporte = reporte,
                        onClick = { onReporteClick(reporte.idReporte) },
                        seleccionado = seleccionadoDetalleId == reporte.idReporte,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

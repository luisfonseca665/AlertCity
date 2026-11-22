package com.example.alertcity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alertcity.R
import com.example.alertcity.data.local.UserSession
import com.example.alertcity.domain.model.SyncStatus
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
    "EN_PROCESO" to "En Proceso",
    "RESUELTO" to "Resuelto"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaReportesScreen(
    viewModel: ReporteViewModel,
    session: UserSession?,
    onCerrarSesion: () -> Unit,
    onReporteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(session?.userId) {
        if (session != null) {
            viewModel.refrescar()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.cargando || uiState.sincronizando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Perfil",
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session?.firstName?.takeIf { it.isNotBlank() }?.let { "Hola, $it" }
                        ?: stringResource(id = R.string.greeting),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = session?.rolLegible ?: stringResource(id = R.string.location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { viewModel.refrescar() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sincronizar con Backend",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onCerrarSesion) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Cerrar sesión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.misActivosCount ?: uiState.totalReportes}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.misActivosCount != null) "Mis Activos" else "Reportes",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                VerticalDivider(modifier = Modifier.height(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.misResueltosCount ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(text = "Resueltos", style = MaterialTheme.typography.labelMedium)
                }
                VerticalDivider(modifier = Modifier.height(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.totalUrgentes}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(text = "Urgentes", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Banner de estado Offline-First y cola de sincronización
        if (!uiState.isOnline || uiState.pendingSyncCount > 0) {
            Surface(
                color = if (!uiState.isOnline) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shape = RoundedCornerShape(10.dp),
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
                                "Modo sin conexión • ${uiState.pendingSyncCount} reporte(s) pendiente(s) de envío"
                            !uiState.isOnline ->
                                "Modo sin conexión • Mostrando datos guardados localmente"
                            else ->
                                "${uiState.pendingSyncCount} reporte(s) pendiente(s) de sincronizar"
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
                            Text(if (uiState.sincronizando) "Enviando..." else "Sincronizar")
                        }
                    }
                }
            }
        }

        // Filtros dinámicos (GET /api/v1/reports?category=...&state=...&urgent=...)
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
                    label = { Text("Todos") }
                )
            }
            item {
                FilterChip(
                    selected = uiState.filtroSoloUrgentes,
                    onClick = { viewModel.onFiltroUrgentesChange(!uiState.filtroSoloUrgentes) },
                    label = { Text("Urgentes") }
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

        uiState.mensajeError?.let { error ->
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

        Text(
            text = if (uiState.tieneFiltrosActivos) {
                "Reportes filtrados (${uiState.listaVisible.size})"
            } else {
                stringResource(id = R.string.my_reports)
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val reportesMostrados = uiState.listaVisible
        if (reportesMostrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (uiState.cargando) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = if (uiState.tieneFiltrosActivos) {
                            "No hay reportes que coincidan con los filtros"
                        } else {
                            stringResource(id = R.string.no_reports)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(reportesMostrados, key = { it.idReporte }) { reporte ->
                    ElevatedCard(
                        onClick = { onReporteClick(reporte.idReporte) },
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reporte.titulo, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = reporte.categoria,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Fecha",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = reporte.fechaFormateada,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Hora",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = reporte.hora,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Estado: ${reporte.estado.etiqueta}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (reporte.estado.progreso == 1.0f) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )
                                if (reporte.syncStatus != SyncStatus.SYNCED) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                                                Icons.Default.ErrorOutline
                                            } else {
                                                Icons.Default.CloudQueue
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = reporte.syncStatus.etiqueta,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (reporte.syncStatus == SyncStatus.SYNC_ERROR) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }
                            }
                            if (reporte.esUrgente) {
                                Text(
                                    "URGENTE",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

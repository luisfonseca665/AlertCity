package com.example.alertcity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alertcity.R
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.util.parsearCoordenadasUbicacion
import com.example.alertcity.ui.components.ImagenPreviewDialog
import com.example.alertcity.ui.components.MapaPreviewDialog
import com.example.alertcity.ui.components.OsmMapCard
import com.example.alertcity.ui.components.PinMapaUi
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.viewmodel.AdminDetalleReporteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDetalleReporteScreen(
    viewModel: AdminDetalleReporteViewModel,
    onBack: () -> Unit,
    mostrarNavegacionAtras: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val reporte = uiState.reporte

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.admin_manage_title),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (mostrarNavegacionAtras) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                    }
                }
            },
            actions = {
                if (reporte != null) {
                    IconButton(onClick = { viewModel.onMostrarConfirmacionEliminarChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.admin_delete_dialog_title),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (reporte == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.accesoDenegadoPorRol) {
                        stringResource(id = R.string.admin_access_denied, uiState.rolLegible)
                    } else {
                        stringResource(id = R.string.detail_not_found)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        val coordenadasParseadas = remember(reporte.latitud, reporte.longitud, reporte.ubicacion) {
            if (reporte.latitud != null && reporte.longitud != null) {
                reporte.latitud to reporte.longitud
            } else {
                parsearCoordenadasUbicacion(reporte.ubicacion)
            }
        }

        val pinesMapa = remember(
            reporte.idReporte,
            reporte.titulo,
            reporte.categoria,
            reporte.estado,
            reporte.esUrgente,
            coordenadasParseadas
        ) {
            coordenadasParseadas?.let { (latitud, longitud) ->
                listOf(
                    PinMapaUi(
                        id = reporte.idReporte,
                        latitud = latitud,
                        longitud = longitud,
                        colorHex = colorHexPorReporte(reporte),
                        titulo = reporte.titulo,
                        descripcion = "${reporte.categoria} • ${reporte.fechaHoraFormateada} • ${reporte.estado.etiqueta}",
                        mostrarInfoWindow = true
                    )
                )
            } ?: emptyList()
        }

        val fotoActual = reporte.fotoUri?.takeIf { it.startsWith("http") }
            ?: reporte.localFotoUri?.takeIf { it.isNotBlank() }
            ?: reporte.fotoUri
        val esResuelto = reporte.estado == EstadoReporte.RESUELTO

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 1. Evidencia fotográfica destacada y previsualizable (igual que en develop)
            if (!fotoActual.isNullOrBlank()) {
                ElevatedCard(
                    onClick = { viewModel.onMostrarPreviewImagenChange(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                    ) {
                        AsyncImage(
                            model = fotoActual,
                            contentDescription = stringResource(id = R.string.create_photo_section),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.52f)
                                        )
                                    )
                                )
                        )

                        Text(
                            text = stringResource(id = R.string.create_photo_section),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.detail_view_image),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Panel administrativo de control de atención
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.admin_attention_control),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (esResuelto) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = "${(reporte.estado.progreso * 100).toInt()}% • ${reporte.estado.etiqueta}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (esResuelto) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { reporte.estado.progreso },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (esResuelto) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(id = R.string.admin_update_status_prompt),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val estados = EstadoReporte.entries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        estados.forEach { estado ->
                            ChipEstatusAdmin(
                                estado = estado,
                                seleccionado = reporte.estado == estado,
                                onClick = { viewModel.cambiarEstado(estado) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Información principal del reporte
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = reporte.categoria,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (reporte.esUrgente) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PriorityHigh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.label_urgent),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = reporte.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reporte.fechaFormateada,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reporte.hora,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Descripción del ciudadano
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.detail_citizen_description_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reporte.descripcion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 5. Ubicación georreferenciada con mapa previsualizable
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.detail_geo_location_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (coordenadasParseadas != null) {
                            TextButton(
                                onClick = { viewModel.onMostrarPreviewMapaChange(true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.action_expand),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (coordenadasParseadas == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = reporte.ubicacion?.takeIf { it.isNotBlank() }
                                        ?: stringResource(id = R.string.detail_no_exact_location),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val (latitud, longitud) = coordenadasParseadas
                        OsmMapCard(
                            latitudCamara = latitud,
                            longitudCamara = longitud,
                            pines = pinesMapa,
                            zoomInicial = 16.5,
                            zoomCamara = 16.5,
                            radioEsquina = 16.dp,
                            etiquetaInferior = reporte.ubicacion,
                            onAmpliarClick = { viewModel.onMostrarPreviewMapaChange(true) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (uiState.mostrarPreviewImagen && !fotoActual.isNullOrBlank()) {
            ImagenPreviewDialog(
                imagenUri = fotoActual,
                titulo = reporte.titulo,
                onDismiss = { viewModel.onMostrarPreviewImagenChange(false) }
            )
        }

        if (uiState.mostrarPreviewMapa && coordenadasParseadas != null) {
            MapaPreviewDialog(
                titulo = reporte.titulo,
                subtitulo = reporte.ubicacion ?: reporte.categoria,
                latitudInicial = coordenadasParseadas.first,
                longitudInicial = coordenadasParseadas.second,
                pines = pinesMapa,
                zoomInicial = 17.0,
                etiquetaInferior = reporte.ubicacion,
                onDismiss = { viewModel.onMostrarPreviewMapaChange(false) }
            )
        }
    }

    if (uiState.mostrarConfirmacionEliminar && reporte != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onMostrarConfirmacionEliminarChange(false) },
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(id = R.string.admin_delete_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(id = R.string.admin_delete_dialog_msg, reporte.titulo))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmarEliminarReporte(onEliminado = onBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.action_delete), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onMostrarConfirmacionEliminarChange(false) }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ChipEstatusAdmin(
    estado: EstadoReporte,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        leadingIcon = if (seleccionado) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        },
        label = { Text(estado.etiqueta) },
        modifier = modifier
    )
}

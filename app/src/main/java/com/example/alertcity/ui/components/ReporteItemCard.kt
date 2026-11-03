package com.example.alertcity.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alertcity.domain.model.Reporte

@Composable
fun ReporteItemCard(
    reporte: Reporte,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    seleccionado: Boolean = false,
    onUbicarEnMapaClick: (() -> Unit)? = null
) {
    if (onUbicarEnMapaClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            border = BorderStroke(
                width = if (seleccionado) 2.dp else 1.dp,
                color = if (seleccionado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (seleccionado) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            ContenidoTarjetaReporte(
                reporte = reporte,
                seleccionado = seleccionado,
                onUbicarEnMapaClick = onUbicarEnMapaClick
            )
        }
    } else {
        ElevatedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            ContenidoTarjetaReporte(
                reporte = reporte,
                seleccionado = false,
                onUbicarEnMapaClick = null
            )
        }
    }
}

@Composable
private fun ContenidoTarjetaReporte(
    reporte: Reporte,
    seleccionado: Boolean,
    onUbicarEnMapaClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.padding(if (onUbicarEnMapaClick != null) 14.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Assignment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(if (onUbicarEnMapaClick != null) 12.dp else 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reporte.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (reporte.esUrgente && onUbicarEnMapaClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    EtiquetaUrgente()
                }
            }
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
        }

        if (onUbicarEnMapaClick != null) {
            IconButton(onClick = onUbicarEnMapaClick) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicar en el mapa",
                    tint = if (seleccionado) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        } else if (reporte.esUrgente) {
            Spacer(modifier = Modifier.width(8.dp))
            EtiquetaUrgente()
        }
    }
}

@Composable
private fun EtiquetaUrgente() {
    Text(
        text = "URGENTE",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

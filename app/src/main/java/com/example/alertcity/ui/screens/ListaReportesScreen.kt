package com.example.alertcity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alertcity.ui.viewmodel.ReporteViewModel

@Composable
fun ListaReportesScreen(viewModel: ReporteViewModel) {
    val reportes by viewModel.listaReportes.collectAsState()

    if (reportes.isEmpty()) {
        Text("No hay reportes aún.", modifier = Modifier.padding(16.dp))
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(reportes) { reporte ->
                Card(modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(reporte.titulo, style = MaterialTheme.typography.titleMedium)
                        Text("${reporte.categoria} • ${reporte.hora}", style = MaterialTheme.typography.bodySmall)
                        if (reporte.esUrgente) {
                            Text("URGENTE", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
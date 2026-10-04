package com.example.alertcity.domain.repository

import com.example.alertcity.domain.model.Reporte
import kotlinx.coroutines.flow.Flow

interface ReporteRepository {
    fun obtenerReportes(): Flow<List<Reporte>>
    suspend fun guardarReporte(reporte: Reporte)
}
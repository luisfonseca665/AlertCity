package com.example.alertcity.domain.repository

import com.example.alertcity.domain.model.Reporte
import kotlinx.coroutines.flow.Flow

interface ReporteRepository {
    fun getAllReports(): Flow<List<Reporte>>
    suspend fun saveReport(report: Reporte)
}
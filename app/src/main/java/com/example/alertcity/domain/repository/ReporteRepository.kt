package com.example.alertcity.domain.repository

import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import kotlinx.coroutines.flow.Flow

data class DashboardResumen(
    val myActiveReportsCount: Int,
    val mySolvedReportsCount: Int,
    val myReports: List<Reporte>,
    val allReports: List<Reporte>
)

interface ReporteRepository {
    fun getAllReports(): Flow<List<Reporte>>

    fun observePendingSyncCount(): Flow<Int>

    suspend fun saveReport(
        report: Reporte,
        latitud: Double? = null,
        longitud: Double? = null
    ): Result<Reporte>

    suspend fun syncPendingReports(): Result<Int>

    suspend fun refreshReports(): Result<Unit>

    suspend fun refreshDashboard(): Result<DashboardResumen>

    suspend fun getFilteredReports(
        category: String? = null,
        state: String? = null,
        urgent: Boolean? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<List<Reporte>>

    suspend fun getNearbyReports(
        lat: Double,
        lng: Double,
        radiusKm: Double = 3.0
    ): Result<List<Reporte>>

    suspend fun updateReportState(
        reportId: String,
        newState: EstadoReporte
    ): Result<Reporte>

    suspend fun updateReport(report: Reporte): Result<Unit>

    suspend fun deleteReport(idReporte: String): Result<Unit>
}

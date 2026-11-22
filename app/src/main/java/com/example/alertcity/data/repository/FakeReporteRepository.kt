package com.example.alertcity.data.repository

import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.DashboardResumen
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

class FakeReporteRepository : ReporteRepository {
    companion object {
        private val reportesEnMemoria = MutableStateFlow(
            listOf(
                Reporte(titulo = "Bache enorme", descripcion = "En la calle principal, casi llegando a la avenida.", categoria = "Baches y Vialidad", esUrgente = true, hora = "10:00", estado = EstadoReporte.EN_PROCESO),
                Reporte(titulo = "Lámpara fundida", descripcion = "Frente al parque, la calle está muy oscura por las noches.", categoria = "Alumbrado Público", esUrgente = false, hora = "14:30", estado = EstadoReporte.RESUELTO),
                Reporte(titulo = "Fuga de agua", descripcion = "El tubo se rompió en la esquina.", categoria = "Fugas de Agua", esUrgente = true, hora = "08:15", estado = EstadoReporte.ENVIADO)
            )
        )
    }

    override fun getAllReports(): Flow<List<Reporte>> = reportesEnMemoria

    override fun observePendingSyncCount(): Flow<Int> = flowOf(0)

    override suspend fun saveReport(
        report: Reporte,
        latitud: Double?,
        longitud: Double?
    ): Result<Reporte> {
        reportesEnMemoria.update { listOf(report) + it }
        return Result.success(report)
    }

    override suspend fun syncPendingReports(): Result<Int> = Result.success(0)

    override suspend fun refreshReports(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun refreshDashboard(): Result<DashboardResumen> {
        val all = reportesEnMemoria.value
        return Result.success(
            DashboardResumen(
                myActiveReportsCount = all.count { it.estado != EstadoReporte.RESUELTO },
                mySolvedReportsCount = all.count { it.estado == EstadoReporte.RESUELTO },
                myReports = all,
                allReports = all
            )
        )
    }

    override suspend fun getFilteredReports(
        category: String?,
        state: String?,
        urgent: Boolean?,
        fromDate: String?,
        toDate: String?
    ): Result<List<Reporte>> = Result.success(reportesEnMemoria.value)

    override suspend fun getNearbyReports(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): Result<List<Reporte>> = Result.success(reportesEnMemoria.value)

    override suspend fun updateReportState(
        reportId: String,
        newState: EstadoReporte
    ): Result<Reporte> {
        var updatedReport: Reporte? = null
        reportesEnMemoria.update { list ->
            list.map {
                if (it.idReporte == reportId) {
                    it.copy(estado = newState).also { u -> updatedReport = u }
                } else {
                    it
                }
            }
        }
        return updatedReport?.let { Result.success(it) }
            ?: Result.failure(Exception("Reporte no encontrado"))
    }
}

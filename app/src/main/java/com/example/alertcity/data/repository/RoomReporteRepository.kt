package com.example.alertcity.data.repository

import com.example.alertcity.data.local.dao.ReporteDao
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.DashboardResumen
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomReporteRepository(
    private val dao: ReporteDao
) : ReporteRepository {

    override fun getAllReports(): Flow<List<Reporte>> {
        return dao.getAllReports().map { entitiesList ->
            entitiesList.map { it.toDomain() }
        }
    }

    override fun observePendingSyncCount(): Flow<Int> = dao.observePendingSyncCount()

    override suspend fun saveReport(
        report: Reporte,
        latitud: Double?,
        longitud: Double?
    ): Result<Reporte> {
        return try {
            dao.addReport(report.toEntity())
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingReports(): Result<Int> = Result.success(0)

    override suspend fun refreshReports(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun refreshDashboard(): Result<DashboardResumen> {
        val all = getAllReports().first()
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
    ): Result<List<Reporte>> = Result.success(getAllReports().first())

    override suspend fun getNearbyReports(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): Result<List<Reporte>> = Result.success(getAllReports().first())

    override suspend fun updateReportState(
        reportId: String,
        newState: EstadoReporte
    ): Result<Reporte> {
        val target = getAllReports().first().find { it.idReporte == reportId }
            ?: return Result.failure(Exception("Reporte no encontrado"))
        val updated = target.copy(estado = newState)
        dao.addReport(updated.toEntity())
        return Result.success(updated)
    }
}

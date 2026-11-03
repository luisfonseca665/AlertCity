package com.example.alertcity.data.repository

import com.example.alertcity.data.local.dao.ReporteDao
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomReporteRepository(
    private val dao: ReporteDao
) : ReporteRepository {

    override fun getAllReports(): Flow<List<Reporte>> {
        return dao.getAllReports().map { entitiesList ->
            entitiesList.map { it.toDomain() }
        }
    }

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

    override suspend fun refreshReports(): Result<Unit> {
        return Result.success(Unit)
    }
}

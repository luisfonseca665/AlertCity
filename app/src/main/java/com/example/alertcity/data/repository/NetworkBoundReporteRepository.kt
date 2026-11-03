package com.example.alertcity.data.repository

import android.util.Log
import com.example.alertcity.data.local.dao.ReporteDao
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.data.mapper.ReporteMapper
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.data.remote.api.AlertCityApi
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.ui.components.parsearCoordenadasUbicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkBoundReporteRepository(
    private val dao: ReporteDao,
    private val api: AlertCityApi
) : ReporteRepository {

    companion object {
        private const val TAG = "AlertCityRepository"
    }

    override fun getAllReports(): Flow<List<Reporte>> {
        return dao.getAllReports().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshReports(): Result<Unit> {
        return try {
            Log.d(TAG, "Consultando reportes desde Backend en ${NetworkConstants.BASE_URL}...")
            val response = api.getReports()
            if (response.isSuccessful) {
                val reportesRemotos = response.body().orEmpty()
                Log.d(TAG, "Reportes recibidos del backend: ${reportesRemotos.size}")
                val entidades = reportesRemotos.map { ReporteMapper.toEntity(it) }
                dao.insertAll(entidades)
                Result.success(Unit)
            } else {
                val errorMsg = "Error HTTP ${response.code()}: ${response.errorBody()?.string()}"
                Log.w(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al conectar con el backend", e)
            Result.failure(e)
        }
    }

    override suspend fun saveReport(
        report: Reporte,
        latitud: Double?,
        longitud: Double?
    ): Result<Reporte> {
        return try {
            val coords = if (latitud != null && longitud != null) {
                latitud to longitud
            } else {
                parsearCoordenadasUbicacion(report.ubicacion)
            }

            val request = ReporteMapper.toCreateRequest(
                userId = NetworkConstants.DEMO_USER_ID,
                titulo = report.titulo,
                descripcion = report.descripcion,
                categoria = report.categoria,
                esUrgente = report.esUrgente,
                latitud = coords?.first,
                longitud = coords?.second,
                referencia = report.ubicacion
            )

            Log.d(TAG, "Enviando POST /api/v1/reports con categoria=${request.category}, lat=${request.latitude}, lon=${request.longitude}")
            val response = api.createReport(request)

            if (response.isSuccessful && response.body() != null) {
                val creadoRemoto = response.body()!!
                Log.d(TAG, "Reporte persistido exitosamente en Backend y PostGIS con ID: ${creadoRemoto.reportId}")
                val entidad = ReporteMapper.toEntity(creadoRemoto)
                dao.addReport(entidad)
                Result.success(entidad.toDomain())
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Error al guardar en backend (${response.code()}): $errorBody"
                Log.w(TAG, errorMsg)
                // Respaldo local en Room para que el usuario no pierda el reporte si el backend responde con error
                dao.addReport(report.toEntity())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo de conexión al enviar reporte, guardando respaldo local en Room", e)
            dao.addReport(report.toEntity())
            Result.failure(e)
        }
    }
}

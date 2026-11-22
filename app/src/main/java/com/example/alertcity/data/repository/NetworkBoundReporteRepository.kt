package com.example.alertcity.data.repository

import android.content.Context
import android.util.Log
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.local.dao.ReporteDao
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.data.mapper.ReporteMapper
import com.example.alertcity.data.media.ImageCompressor
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.data.remote.NetworkMonitor
import com.example.alertcity.data.remote.api.AlertCityApi
import com.example.alertcity.data.remote.model.ApiErrorParser
import com.example.alertcity.data.remote.model.UpdateReportStateRequest
import com.example.alertcity.data.worker.SyncScheduler
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.model.SyncStatus
import com.example.alertcity.domain.repository.DashboardResumen
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.domain.util.GeoUtils
import com.example.alertcity.ui.components.parsearCoordenadasUbicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NetworkBoundReporteRepository(
    private val context: Context,
    private val dao: ReporteDao,
    private val api: AlertCityApi,
    private val sessionManager: SessionManager? = null,
    private val networkMonitor: NetworkMonitor? = null
) : ReporteRepository {

    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "AlertCityRepository"
    }

    override fun getAllReports(): Flow<List<Reporte>> {
        return dao.getAllReports().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observePendingSyncCount(): Flow<Int> {
        return dao.observePendingSyncCount()
    }

    override suspend fun saveReport(
        report: Reporte,
        latitud: Double?,
        longitud: Double?
    ): Result<Reporte> {
        val coords = if (latitud != null && longitud != null) {
            latitud to longitud
        } else if (report.latitud != null && report.longitud != null) {
            report.latitud to report.longitud
        } else {
            parsearCoordenadasUbicacion(report.ubicacion)
        }

        val localPhoto = report.localFotoUri
            ?: report.fotoUri?.takeIf { it.startsWith("file://") || it.startsWith("content://") }

        val pendingReport = report.copy(
            latitud = coords?.first,
            longitud = coords?.second,
            syncStatus = SyncStatus.SYNC_PENDING,
            localFotoUri = localPhoto,
            syncError = null
        )

        // 1. Single Source of Truth: persistir de inmediato en Room como SYNC_PENDING
        val pendingEntity = pendingReport.toEntity()
        dao.addReport(pendingEntity)

        // 2. Encolar WorkManager con restricción NetworkType.CONNECTED
        SyncScheduler.enqueueOneTimeSync(context)

        // 3. Si hay conectividad en este instante, intentar envío inmediato sin fallar si cae la red
        if (networkMonitor?.checkCurrentlyOnline() != false) {
            syncPendingReports()
        }

        val finalDomain = dao.getAllReportsOnce()
            .find { it.idReporte == pendingReport.idReporte }
            ?.toDomain()
            ?: pendingReport

        return Result.success(finalDomain)
    }

    override suspend fun syncPendingReports(): Result<Int> = syncMutex.withLock {
        val pendingList = dao.getPendingSyncReports()
        if (pendingList.isEmpty()) {
            return@withLock Result.success(0)
        }

        if (networkMonitor != null && !networkMonitor.checkCurrentlyOnline()) {
            return@withLock Result.failure(Exception("Sin conexión de red para sincronizar."))
        }

        var syncedCount = 0
        var lastTransientError: Exception? = null

        for (entity in pendingList) {
            try {
                val coords = if (entity.latitud != null && entity.longitud != null) {
                    entity.latitud to entity.longitud
                } else {
                    parsearCoordenadasUbicacion(entity.ubicacion)
                }

                val request = ReporteMapper.toCreateRequest(
                    userId = null,
                    titulo = entity.titulo,
                    descripcion = entity.descripcion,
                    categoria = entity.categoria,
                    esUrgente = entity.esUrgente,
                    latitud = coords?.first,
                    longitud = coords?.second,
                    referencia = entity.ubicacion
                )

                Log.d(TAG, "Sincronizando reporte pendiente ${entity.idReporte}...")
                val response = api.createReport(request)

                if (!response.isSuccessful || response.body() == null) {
                    val code = response.code()
                    val errorMsg = ApiErrorParser.formatMessage(response, "Error al sincronizar reporte")
                    if (isPermanentClientError(code)) {
                        Log.w(TAG, "Error permanente ($code) en reporte ${entity.idReporte}: $errorMsg")
                        dao.updateSyncStatus(entity.idReporte, SyncStatus.SYNC_ERROR, errorMsg)
                        continue
                    } else {
                        Log.w(TAG, "Error transitorio ($code) al sincronizar ${entity.idReporte}: $errorMsg")
                        lastTransientError = Exception(errorMsg)
                        break
                    }
                }

                var reporteRemoto = response.body()!!
                val localPhoto = entity.localFotoUri
                    ?: entity.fotoUri?.takeIf { it.startsWith("file://") || it.startsWith("content://") }

                if (!localPhoto.isNullOrBlank()) {
                    val mediaPart = ImageCompressor.prepareMultipartPart(context, localPhoto)
                    if (mediaPart != null) {
                        val (part, tempFile) = mediaPart
                        try {
                            val uploadResponse = api.uploadReportMedia(reporteRemoto.reportId, part)
                            if (uploadResponse.isSuccessful && uploadResponse.body() != null) {
                                reporteRemoto = uploadResponse.body()!!
                                Log.d(TAG, "Evidencia fotográfica sincronizada: ${reporteRemoto.imageUrl}")
                            } else {
                                Log.w(
                                    TAG,
                                    "No se pudo subir foto en sync: ${ApiErrorParser.formatMessage(uploadResponse)}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Excepción al subir foto durante sincronización", e)
                        } finally {
                            tempFile?.delete()
                        }
                    }
                }

                val syncedEntity = ReporteMapper.toEntity(reporteRemoto).let { mapped ->
                    if (mapped.fotoUri.isNullOrBlank() && !localPhoto.isNullOrBlank()) {
                        mapped.copy(
                            fotoUri = localPhoto,
                            localFotoUri = localPhoto,
                            syncStatus = SyncStatus.SYNCED,
                            syncError = null
                        )
                    } else {
                        mapped.copy(
                            localFotoUri = localPhoto,
                            syncStatus = SyncStatus.SYNCED,
                            syncError = null
                        )
                    }
                }

                dao.replacePendingWithSynced(
                    localId = entity.idReporte,
                    syncedEntity = syncedEntity
                )
                syncedCount++
            } catch (e: Exception) {
                Log.w(TAG, "Fallo de red sincronizando reporte ${entity.idReporte}", e)
                if (entity.syncStatus == SyncStatus.SYNC_ERROR) {
                    dao.updateSyncStatus(entity.idReporte, SyncStatus.SYNC_PENDING, null)
                }
                lastTransientError = e
                break
            }
        }

        return@withLock if (lastTransientError != null) {
            Result.failure(lastTransientError)
        } else {
            Result.success(syncedCount)
        }
    }

    override suspend fun refreshReports(): Result<Unit> {
        return try {
            syncPendingReports()
            Log.d(TAG, "Consultando reportes desde Backend en ${NetworkConstants.BASE_URL}...")
            val response = api.getReports()
            if (response.isSuccessful) {
                val reportesRemotos = response.body().orEmpty()
                Log.d(TAG, "Reportes recibidos del backend: ${reportesRemotos.size}")
                val entidades = reportesRemotos.map { ReporteMapper.toEntity(it) }
                dao.upsertRemoteReports(entidades)
                Result.success(Unit)
            } else {
                val errorMsg = ApiErrorParser.formatMessage(response, "Error al consultar reportes")
                Log.w(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al conectar con el backend", e)
            Result.failure(Exception("No se pudo conectar con el servidor. Mostrando datos locales."))
        }
    }

    override suspend fun refreshDashboard(): Result<DashboardResumen> {
        return try {
            syncPendingReports()
            val response = api.getDashboard()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val allEntities = body.allReports.map { ReporteMapper.toEntity(it) }
                val myEntities = body.myReports.map { ReporteMapper.toEntity(it) }
                val combined = (allEntities + myEntities).distinctBy { it.idReporte }
                if (combined.isNotEmpty()) {
                    dao.upsertRemoteReports(combined)
                }
                Result.success(
                    DashboardResumen(
                        myActiveReportsCount = body.myActiveReportsCount,
                        mySolvedReportsCount = body.mySolvedReportsCount,
                        myReports = body.myReports.map { ReporteMapper.toDomain(it) },
                        allReports = body.allReports.map { ReporteMapper.toDomain(it) }
                    )
                )
            } else {
                val errorMsg = ApiErrorParser.formatMessage(response, "Error al cargar el dashboard")
                Log.w(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sin conexión para cargar dashboard remoto; usando caché local de Room", e)
            Result.success(buildLocalDashboardFallback())
        }
    }

    override suspend fun getFilteredReports(
        category: String?,
        state: String?,
        urgent: Boolean?,
        fromDate: String?,
        toDate: String?
    ): Result<List<Reporte>> {
        return try {
            val categoryEnum = category?.takeIf { it.isNotBlank() }?.let { ReporteMapper.mapCategoryToBackendEnum(it) }
            val response = api.getReports(
                category = categoryEnum,
                state = state?.takeIf { it.isNotBlank() },
                urgent = urgent,
                fromDate = fromDate,
                toDate = toDate
            )
            if (response.isSuccessful) {
                val reportes = response.body().orEmpty().map { ReporteMapper.toDomain(it) }
                Result.success(reportes)
            } else {
                val errorMsg = ApiErrorParser.formatMessage(response, "Error al filtrar reportes")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Filtrando reportes localmente en Room (modo offline)", e)
            val localFiltered = filterLocalReports(category, state, urgent)
            Result.success(localFiltered)
        }
    }

    override suspend fun getNearbyReports(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): Result<List<Reporte>> {
        return try {
            val response = api.getNearbyReports(lat = lat, lng = lng, radiusKm = radiusKm)
            if (response.isSuccessful) {
                val reportesRemotos = response.body().orEmpty()
                if (reportesRemotos.isNotEmpty()) {
                    dao.upsertRemoteReports(reportesRemotos.map { ReporteMapper.toEntity(it) })
                }
                Result.success(reportesRemotos.map { ReporteMapper.toDomain(it) })
            } else {
                val errorMsg = ApiErrorParser.formatMessage(response, "Error al consultar reportes cercanos")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Calculando reportes cercanos offline con Haversine sobre Room", e)
            val nearbyLocal = findNearbyLocalReports(lat, lng, radiusKm)
            Result.success(nearbyLocal)
        }
    }

    override suspend fun updateReportState(
        reportId: String,
        newState: EstadoReporte
    ): Result<Reporte> {
        return try {
            val backendState = ReporteMapper.mapDomainStateToBackendEnum(newState)
            val response = api.updateReportState(
                reportId = reportId,
                request = UpdateReportStateRequest(state = backendState)
            )
            if (response.isSuccessful && response.body() != null) {
                val actualizado = response.body()!!
                val entidad = ReporteMapper.toEntity(actualizado)
                dao.addReport(entidad)
                Result.success(entidad.toDomain())
            } else {
                val errorMsg = ApiErrorParser.formatMessage(response, "No se pudo actualizar el estado")
                Log.w(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al actualizar estado del reporte", e)
            Result.failure(Exception("No se pudo conectar con el servidor."))
        }
    }

    private suspend fun buildLocalDashboardFallback(): DashboardResumen {
        val allLocal = dao.getAllReportsOnce().map { it.toDomain() }
        val currentUserId = sessionManager?.currentSession?.userId
        val myLocal = if (!currentUserId.isNullOrBlank()) {
            allLocal.filter { it.idUsuario == currentUserId }
        } else {
            allLocal
        }
        return DashboardResumen(
            myActiveReportsCount = myLocal.count { it.estado != EstadoReporte.RESUELTO },
            mySolvedReportsCount = myLocal.count { it.estado == EstadoReporte.RESUELTO },
            myReports = myLocal,
            allReports = allLocal
        )
    }

    private suspend fun filterLocalReports(
        category: String?,
        state: String?,
        urgent: Boolean?
    ): List<Reporte> {
        val expectedCategory = category?.takeIf { it.isNotBlank() }?.let {
            ReporteMapper.mapCategoryToBackendEnum(it)
        }
        val expectedState = state?.takeIf { it.isNotBlank() }?.trim()?.uppercase()

        return dao.getAllReportsOnce()
            .map { it.toDomain() }
            .filter { reporte ->
                val matchCategory = expectedCategory == null ||
                    ReporteMapper.mapCategoryToBackendEnum(reporte.categoria) == expectedCategory
                val matchState = expectedState == null ||
                    ReporteMapper.mapDomainStateToBackendEnum(reporte.estado) == expectedState
                val matchUrgent = urgent == null || reporte.esUrgente == urgent
                matchCategory && matchState && matchUrgent
            }
    }

    private suspend fun findNearbyLocalReports(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): List<Reporte> {
        return dao.getAllReportsOnce()
            .map { it.toDomain() }
            .mapNotNull { reporte ->
                val coords = if (reporte.latitud != null && reporte.longitud != null) {
                    reporte.latitud to reporte.longitud
                } else {
                    parsearCoordenadasUbicacion(reporte.ubicacion)
                }
                coords?.let { (rLat, rLng) ->
                    val distancia = GeoUtils.haversineDistanceKm(lat, lng, rLat, rLng)
                    if (distancia <= radiusKm) reporte to distancia else null
                }
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun isPermanentClientError(httpCode: Int): Boolean {
        return httpCode in 400..499 && httpCode != 401 && httpCode != 408 && httpCode != 429
    }
}

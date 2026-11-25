package com.example.alertcity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.alertcity.data.local.entity.ReporteEntity
import com.example.alertcity.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Query("SELECT * FROM reportes ORDER BY fechaCreacion DESC, hora DESC")
    fun getAllReports(): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes ORDER BY fechaCreacion DESC, hora DESC")
    suspend fun getAllReportsOnce(): List<ReporteEntity>

    @Query("SELECT * FROM reportes WHERE syncStatus IN ('SYNC_PENDING', 'SYNC_ERROR') ORDER BY fechaCreacion ASC")
    suspend fun getPendingSyncReports(): List<ReporteEntity>

    @Query("SELECT * FROM reportes WHERE syncStatus = 'SYNCED' AND localFotoUri IS NOT NULL AND (fotoUri IS NULL OR fotoUri NOT LIKE 'http%')")
    suspend fun getSyncedReportsWithPendingPhoto(): List<ReporteEntity>

    @Query("SELECT COUNT(*) FROM reportes WHERE syncStatus IN ('SYNC_PENDING', 'SYNC_ERROR') OR (syncStatus = 'SYNCED' AND localFotoUri IS NOT NULL AND (fotoUri IS NULL OR fotoUri NOT LIKE 'http%'))")
    fun observePendingSyncCount(): Flow<Int>

    @Query("SELECT idReporte FROM reportes WHERE syncStatus != 'SYNCED'")
    suspend fun getNonSyncedIds(): List<String>

    @Query("UPDATE reportes SET syncStatus = :status, syncError = :errorMsg WHERE idReporte = :idReporte")
    suspend fun updateSyncStatus(idReporte: String, status: SyncStatus, errorMsg: String?)

    @Query("DELETE FROM reportes WHERE idReporte = :idReporte")
    suspend fun deleteReportById(idReporte: String)

    @Query("DELETE FROM reportes WHERE syncStatus = 'SYNCED'")
    suspend fun deleteAllSyncedReports()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addReport(reporte: ReporteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reportes: List<ReporteEntity>)

    @Update
    suspend fun updateReport(reporte: ReporteEntity)

    @Transaction
    suspend fun replacePendingWithSynced(localId: String, syncedEntity: ReporteEntity) {
        if (localId != syncedEntity.idReporte) {
            deleteReportById(localId)
        }
        addReport(syncedEntity)
    }

    @Transaction
    suspend fun upsertRemoteReports(remoteEntities: List<ReporteEntity>) {
        if (remoteEntities.isEmpty()) return
        val protectedIds = getNonSyncedIds().toSet()
        val safeToInsert = remoteEntities.filterNot { it.idReporte in protectedIds }
        if (safeToInsert.isNotEmpty()) {
            insertAll(safeToInsert)
        }
    }

    @Transaction
    suspend fun replaceAllSyncedReports(remoteEntities: List<ReporteEntity>) {
        val protectedIds = getNonSyncedIds().toSet()
        val pendingLocalPhotosById = getSyncedReportsWithPendingPhoto()
            .associate { it.idReporte to it.localFotoUri }
        deleteAllSyncedReports()
        val safeToInsert = remoteEntities
            .filterNot { it.idReporte in protectedIds }
            .map { entity ->
                val pendingLocal = pendingLocalPhotosById[entity.idReporte]
                if (entity.fotoUri.isNullOrBlank() && !pendingLocal.isNullOrBlank()) {
                    entity.copy(fotoUri = pendingLocal, localFotoUri = pendingLocal)
                } else {
                    entity
                }
            }
        if (safeToInsert.isNotEmpty()) {
            insertAll(safeToInsert)
        }
    }
}

package com.example.alertcity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alertcity.data.local.entity.ReporteEntity
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Query("SELECT * FROM reportes ORDER BY hora DESC")
    fun getAllReports(): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes WHERE idReporte = :id")
    suspend fun getReportById(id: String ): ReporteEntity?

    @Query("SELECT * FROM reportes WHERE idUsuario = :idUsuario ORDER BY hora DESC")
    fun getAllReportsByUser(idUsuario: String): Flow<List<ReporteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addReport(reporte: ReporteEntity)
}
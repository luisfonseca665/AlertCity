package com.example.alertcity.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.model.SyncStatus

@Entity(tableName = "reportes")
data class ReporteEntity(
    @PrimaryKey
    @ColumnInfo(name = "idReporte") val idReporte: String,
    @ColumnInfo(name = "idUsuario") val idUsuario: String,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "categoria") val categoria: String,
    @ColumnInfo(name = "esUrgente") val esUrgente: Boolean,
    @ColumnInfo(name = "hora") val hora: String,
    @ColumnInfo(name = "fechaCreacion", defaultValue = "0") val fechaCreacion: Long = 0L,
    @ColumnInfo(name = "fotoUri") val fotoUri: String?,
    @ColumnInfo(name = "ubicacion") val ubicacion: String?,
    @ColumnInfo(name = "estado") val estado: EstadoReporte,
    @ColumnInfo(name = "latitud") val latitud: Double? = null,
    @ColumnInfo(name = "longitud") val longitud: Double? = null,
    @ColumnInfo(name = "syncStatus", defaultValue = "'SYNCED'") val syncStatus: SyncStatus = SyncStatus.SYNCED,
    @ColumnInfo(name = "localFotoUri") val localFotoUri: String? = null,
    @ColumnInfo(name = "syncError") val syncError: String? = null
)

fun ReporteEntity.toDomain(): Reporte = Reporte(
    idReporte = idReporte,
    idUsuario = idUsuario,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    esUrgente = esUrgente,
    hora = hora,
    fechaCreacion = fechaCreacion,
    fotoUri = NetworkConstants.resolveMediaUrl(fotoUri) ?: localFotoUri,
    ubicacion = ubicacion,
    estado = estado,
    latitud = latitud,
    longitud = longitud,
    syncStatus = syncStatus,
    localFotoUri = localFotoUri,
    syncError = syncError
)

fun Reporte.toEntity(): ReporteEntity = ReporteEntity(
    idReporte = idReporte,
    idUsuario = idUsuario,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    esUrgente = esUrgente,
    hora = hora,
    fechaCreacion = fechaCreacion,
    fotoUri = fotoUri,
    ubicacion = ubicacion,
    estado = estado,
    latitud = latitud,
    longitud = longitud,
    syncStatus = syncStatus,
    localFotoUri = localFotoUri,
    syncError = syncError
)

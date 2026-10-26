package com.example.alertcity.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte

@Entity(
    tableName = "reportes"

/* VAMOS A QUITAR ESTO POR EL MOMENTO PORQUE SOLO TENEMOS UN USUARIO
,
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["idUsuario"])]*/
)
data class ReporteEntity (
    @PrimaryKey
    @ColumnInfo("idReporte") val idReporte: String,
    @ColumnInfo("idUsuario") val idUsuario: String,
    @ColumnInfo("titulo") val titulo: String,
    @ColumnInfo("descripcion") val descripcion: String,
    @ColumnInfo("categoria") val categoria: String,
    @ColumnInfo("esUrgente") val esUrgente: Boolean,
    @ColumnInfo("hora") val hora: String,
    @ColumnInfo("fotoUri") val fotoUri: String?,
    @ColumnInfo("ubicacion") val ubicacion: String?,
    @ColumnInfo("estado") val estado: EstadoReporte,
)

fun ReporteEntity.toDomain(): Reporte = Reporte(
    idReporte = idReporte,
    idUsuario = idUsuario,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    esUrgente = esUrgente,
    hora = hora,
    fotoUri = fotoUri,
    ubicacion = ubicacion,
    estado = estado
)

fun Reporte.toEntity(): ReporteEntity = ReporteEntity(
    idReporte = idReporte,
    idUsuario = idUsuario,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    esUrgente = esUrgente,
    hora = hora,
    fotoUri = fotoUri,
    ubicacion = ubicacion,
    estado = estado
)
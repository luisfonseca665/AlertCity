package com.example.alertcity.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Reporte(
    val idReporte: String = UUID.randomUUID().toString(),
    val idUsuario: String = "1",
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val esUrgente: Boolean,
    val hora: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fotoUri: String? = null,
    val ubicacion: String? = null,
    val estado: EstadoReporte = EstadoReporte.ENVIADO,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localFotoUri: String? = null,
    val syncError: String? = null
) {
    val fechaFormateada: String
        get() = if (fechaCreacion > 0L) {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fechaCreacion))
        } else {
            "Sin fecha"
        }

    val fechaHoraFormateada: String
        get() = if (fechaCreacion > 0L) {
            "$fechaFormateada • $hora"
        } else {
            hora
        }
}

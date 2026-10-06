package com.example.alertcity.domain.model


import java.util.UUID

enum class EstadoReporte(val etiqueta: String, val progreso: Float) {
    ENVIADO("Enviado", 0.25f),
    REVISION("En Revisión", 0.50f),
    EN_PROCESO("Atendiendo", 0.75f),
    RESUELTO("Resuelto", 1.0f)
}
data class Reporte(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val esUrgente: Boolean,
    val hora: String,
    val fotoUri: String? = null,
    val ubicacion: String? = null,
    val estado: EstadoReporte = EstadoReporte.ENVIADO
)
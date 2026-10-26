package com.example.alertcity.domain.model

enum class EstadoReporte(val etiqueta: String, val progreso: Float) {
    ENVIADO("Enviado", 0.25f),
    REVISION("En Revisión", 0.50f),
    EN_PROCESO("Atendiendo", 0.75f),
    RESUELTO("Resuelto", 1.0f)
}
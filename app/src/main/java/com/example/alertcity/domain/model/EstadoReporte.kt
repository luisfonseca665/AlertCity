package com.example.alertcity.domain.model

enum class EstadoReporte(val etiqueta: String, val progreso: Float) {
    ENVIADO("Enviado", 0.33f),
    EN_PROCESO("Atendiendo", 0.66f),
    RESUELTO("Resuelto", 1.0f)
}

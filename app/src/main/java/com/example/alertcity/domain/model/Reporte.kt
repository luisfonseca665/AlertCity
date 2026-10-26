package com.example.alertcity.domain.model

import java.util.UUID

data class Reporte(
    val idReporte: String = UUID.randomUUID().toString(),
    val idUsuario: String = "1",
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val esUrgente: Boolean,
    val hora: String,
    val fotoUri: String? = null,
    val ubicacion: String? = null,
    val estado: EstadoReporte = EstadoReporte.ENVIADO
)
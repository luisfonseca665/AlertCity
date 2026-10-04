package com.example.alertcity.domain.model


import java.util.UUID

data class Reporte(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val esUrgente: Boolean,
    val hora: String
)
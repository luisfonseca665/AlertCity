package com.example.alertcity.domain.model

import android.R
import java.util.UUID


data class Usuario(
    val idUsuario: String,
    val usuario: String,
    val contrasena: String,
    val nombre: String,
    val apellido: String,
    val direccion: String
)
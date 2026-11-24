package com.example.alertcity.domain.repository

import com.example.alertcity.domain.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UsuarioRepository {
    val usuarioActual: StateFlow<Usuario?>

    fun getAllUsuarios(): Flow<List<Usuario>>

    suspend fun login(usuario: String, contrasena: String): Result<Usuario>

    fun cerrarSesion()

    suspend fun saveUsuario(usuario: Usuario): Result<Unit>

    suspend fun deleteUsuario(idUsuario: String): Result<Unit>

    suspend fun ensureSeedUsers()
}

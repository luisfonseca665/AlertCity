package com.example.alertcity.domain.repository

import com.example.alertcity.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun getAllUsuarios(): Flow<List<Usuario>>

    suspend fun saveUsuario(usuario: Usuario): Result<Unit>

    suspend fun deleteUsuario(idUsuario: String): Result<Unit>

    suspend fun ensureSeedUsers()
}

package com.example.alertcity.data.repository

import com.example.alertcity.data.local.dao.UsuarioDao
import com.example.alertcity.data.local.entity.UsuarioEntity
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.domain.model.Usuario
import com.example.alertcity.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUsuarioRepository(
    private val dao: UsuarioDao
) : UsuarioRepository {

    override fun getAllUsuarios(): Flow<List<Usuario>> {
        return dao.getAllUsuarios().map { entidades ->
            entidades.map { it.toDomain() }
        }
    }

    override suspend fun saveUsuario(usuario: Usuario): Result<Unit> {
        return try {
            dao.insertUsuario(usuario.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUsuario(idUsuario: String): Result<Unit> {
        return try {
            dao.deleteUsuarioById(idUsuario)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureSeedUsers() {
        if (dao.countUsuarios() > 0) return

        val usuariosSemilla = listOf(
            UsuarioEntity(
                idUsuario = NetworkConstants.DEMO_USER_ID,
                usuario = "laura.sanchez",
                contrasena = "••••••••",
                nombre = "Laura",
                apellido = "Sánchez",
                direccion = "Zona Centro, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "22222222-2222-4222-a222-222222222222",
                usuario = "luis.enrique",
                contrasena = "••••••••",
                nombre = "Luis Enrique",
                apellido = "Fonseca",
                direccion = "Col. Independencia, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "33333333-3333-4333-a333-333333333333",
                usuario = "carlos.ramirez",
                contrasena = "••••••••",
                nombre = "Carlos",
                apellido = "Ramírez",
                direccion = "Av. Morelos #214, Uriangato, Gto."
            )
        )
        dao.insertAll(usuariosSemilla)
    }
}

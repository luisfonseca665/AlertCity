package com.example.alertcity.data.repository

import com.example.alertcity.data.local.dao.UsuarioDao
import com.example.alertcity.data.local.entity.UsuarioEntity
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.domain.model.Usuario
import com.example.alertcity.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class RoomUsuarioRepository(
    private val dao: UsuarioDao
) : UsuarioRepository {

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    override val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    override fun getAllUsuarios(): Flow<List<Usuario>> {
        return dao.getAllUsuarios().map { entidades ->
            entidades.map { it.toDomain() }
        }
    }

    override suspend fun login(usuario: String, contrasena: String): Result<Usuario> {
        return try {
            ensureSeedUsers()
            val encontrado = dao.login(usuario.trim(), contrasena)
            if (encontrado != null) {
                val domainUser = encontrado.toDomain()
                _usuarioActual.value = domainUser
                Result.success(domainUser)
            } else {
                Result.failure(IllegalArgumentException("Usuario o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cerrarSesion() {
        _usuarioActual.value = null
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
                contrasena = "123456",
                nombre = "Laura",
                apellido = "Sánchez",
                direccion = "Zona Centro, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "22222222-2222-4222-a222-222222222222",
                usuario = "luis.enrique",
                contrasena = "123456",
                nombre = "Luis Enrique",
                apellido = "Fonseca",
                direccion = "Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "33333333-3333-4333-a333-333333333333",
                usuario = "carlos.ramirez",
                contrasena = "123456",
                nombre = "Carlos",
                apellido = "Ramírez",
                direccion = "Av. Morelos #214, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "99999999-9999-4999-a999-999999999999",
                usuario = "admin.alertcity",
                contrasena = "123456",
                nombre = "Administrador",
                apellido = "Municipal",
                direccion = "Presidencia Municipal, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444401",
                usuario = "carlos_vialidad",
                contrasena = "password",
                nombre = "Carlos",
                apellido = "Vialidad",
                direccion = "Obras Públicas, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444402",
                usuario = "maria_alumbrado",
                contrasena = "password",
                nombre = "María",
                apellido = "Alumbrado",
                direccion = "Alumbrado Público, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444403",
                usuario = "roberto_agua",
                contrasena = "password",
                nombre = "Roberto",
                apellido = "Agua y Drenaje",
                direccion = "SMAPAU, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444404",
                usuario = "ana_limpieza",
                contrasena = "password",
                nombre = "Ana",
                apellido = "Limpieza",
                direccion = "Servicios Municipales, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444405",
                usuario = "jorge_seguridad",
                contrasena = "password",
                nombre = "Jorge",
                apellido = "Seguridad",
                direccion = "Seguridad Pública, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "11111111-1111-4111-a111-111111111111",
                usuario = "laura_ciudadana",
                contrasena = "password",
                nombre = "Laura",
                apellido = "Sánchez",
                direccion = "Zona Centro, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444406",
                usuario = "patricia_verde",
                contrasena = "password",
                nombre = "Patricia",
                apellido = "Morales",
                direccion = "Parques y Jardines, Uriangato, Gto."
            ),
            UsuarioEntity(
                idUsuario = "44444444-4444-4444-a444-444444444407",
                usuario = "esteban_animales",
                contrasena = "password",
                nombre = "Esteban",
                apellido = "Ramírez",
                direccion = "Protección Animal, Uriangato, Gto."
            )
        )
        dao.insertAll(usuariosSemilla)
    }
}

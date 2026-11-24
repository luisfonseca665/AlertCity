package com.example.alertcity.data.repository

import android.util.Log
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.remote.api.AlertCityApi
import com.example.alertcity.data.remote.model.ApiErrorParser
import com.example.alertcity.data.remote.model.AuthResponse
import com.example.alertcity.data.remote.model.LoginRequest
import com.example.alertcity.data.remote.model.RegisterRequest
import com.example.alertcity.domain.model.Usuario
import com.example.alertcity.domain.repository.UsuarioRepository
import java.util.UUID

class AuthRepository(
    private val api: AlertCityApi,
    private val sessionManager: SessionManager,
    private val usuarioRepository: UsuarioRepository? = null
) {

    suspend fun login(usernameOrEmail: String, password: String): Result<AuthResponse> {
        val cleanInput = usernameOrEmail.trim()
        return try {
            val response = api.login(
                LoginRequest(
                    usernameOrEmail = cleanInput,
                    password = password
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                sessionManager.saveSession(auth)
                Result.success(auth)
            } else {
                Result.failure(Exception(ApiErrorParser.formatMessage(response, "No se pudo iniciar sesión")))
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Servidor no disponible, intentando login local en Room", e)
            val localRepo = usuarioRepository
            if (localRepo != null) {
                val localResult = localRepo.login(cleanInput, password)
                val usuarioLocal = localResult.getOrNull()
                if (usuarioLocal != null) {
                    val rolInferido = inferirRolDesdeUsuario(usuarioLocal.usuario)
                    val localAuth = AuthResponse(
                        accessToken = "local-offline-token-${usuarioLocal.idUsuario}",
                        refreshToken = "local-offline-refresh-${usuarioLocal.idUsuario}",
                        tokenType = "Bearer",
                        expiresIn = 86400L,
                        userId = usuarioLocal.idUsuario,
                        username = usuarioLocal.usuario,
                        email = "${usuarioLocal.usuario}@alertcity.local",
                        rol = rolInferido,
                        firstName = usuarioLocal.nombre,
                        lastName = usuarioLocal.apellido
                    )
                    sessionManager.saveSession(localAuth)
                    return Result.success(localAuth)
                }
            }
            val detalle = e.localizedMessage ?: e.javaClass.simpleName
            Result.failure(Exception("No se pudo conectar con el servidor ($detalle). Verifica tu red o usa un usuario local (ej. laura.sanchez / 123456)."))
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        direccion: String = ""
    ): Result<AuthResponse> {
        return try {
            val response = api.register(
                RegisterRequest(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    firstName = firstName.trim(),
                    lastName = lastName.trim()
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                usuarioRepository?.saveUsuario(
                    Usuario(
                        idUsuario = auth.userId,
                        usuario = auth.username,
                        contrasena = password,
                        nombre = auth.firstName,
                        apellido = auth.lastName,
                        direccion = direccion.trim()
                    )
                )
                sessionManager.saveSession(auth)
                Result.success(auth)
            } else {
                Result.failure(Exception(ApiErrorParser.formatMessage(response, "No se pudo completar el registro")))
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Servidor no disponible, registrando usuario localmente en Room", e)
            val localRepo = usuarioRepository
            if (localRepo != null) {
                val nuevoId = UUID.randomUUID().toString()
                val nuevoUsuario = Usuario(
                    idUsuario = nuevoId,
                    usuario = username.trim(),
                    contrasena = password,
                    nombre = firstName.trim(),
                    apellido = lastName.trim(),
                    direccion = direccion.trim()
                )
                localRepo.saveUsuario(nuevoUsuario)
                val localAuth = AuthResponse(
                    accessToken = "local-offline-token-$nuevoId",
                    refreshToken = "local-offline-refresh-$nuevoId",
                    tokenType = "Bearer",
                    expiresIn = 86400L,
                    userId = nuevoId,
                    username = nuevoUsuario.usuario,
                    email = email.trim().ifBlank { "${nuevoUsuario.usuario}@alertcity.local" },
                    rol = "CIUDADANO",
                    firstName = nuevoUsuario.nombre,
                    lastName = nuevoUsuario.apellido
                )
                sessionManager.saveSession(localAuth)
                return Result.success(localAuth)
            }
            val detalle = e.localizedMessage ?: e.javaClass.simpleName
            Result.failure(Exception("No se pudo conectar con el servidor ($detalle). Verifica tu red y la IP del backend."))
        }
    }

    fun logout() {
        usuarioRepository?.cerrarSesion()
        sessionManager.clearSession()
    }

    private fun inferirRolDesdeUsuario(username: String): String {
        val u = username.trim().lowercase()
        return when {
            u.contains("vialidad") -> "VIALIDAD"
            u.contains("alumbrado") -> "ALUMBRADO"
            u.contains("agua") || u.contains("drenaje") -> "AGUA_DRENAJE"
            u.contains("limpieza") -> "LIMPIEZA"
            u.contains("seguridad") -> "SEGURIDAD"
            u.contains("verde") || u.contains("parque") -> "AREAS_VERDES"
            u.contains("animal") -> "ANIMALES"
            u.contains("admin") -> "ADMIN"
            else -> "CIUDADANO"
        }
    }
}

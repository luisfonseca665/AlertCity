package com.example.alertcity.data.repository

import android.util.Log
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.remote.api.AlertCityApi
import com.example.alertcity.data.remote.model.ApiErrorParser
import com.example.alertcity.data.remote.model.AuthResponse
import com.example.alertcity.data.remote.model.LoginRequest
import com.example.alertcity.data.remote.model.RegisterRequest

class AuthRepository(
    private val api: AlertCityApi,
    private val sessionManager: SessionManager
) {

    suspend fun login(usernameOrEmail: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(
                LoginRequest(
                    usernameOrEmail = usernameOrEmail.trim(),
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
            Log.e("AuthRepository", "Error en login", e)
            val detalle = e.localizedMessage ?: e.javaClass.simpleName
            Result.failure(Exception("No se pudo conectar con el servidor ($detalle). Verifica tu red y la IP del backend."))
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String
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
                sessionManager.saveSession(auth)
                Result.success(auth)
            } else {
                Result.failure(Exception(ApiErrorParser.formatMessage(response, "No se pudo completar el registro")))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en login", e)
            val detalle = e.localizedMessage ?: e.javaClass.simpleName
            Result.failure(Exception("No se pudo conectar con el servidor ($detalle). Verifica tu red y la IP del backend."))
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}

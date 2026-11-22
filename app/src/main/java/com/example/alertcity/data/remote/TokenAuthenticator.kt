package com.example.alertcity.data.remote

import android.util.Log
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.remote.api.AlertCityApi
import com.example.alertcity.data.remote.model.RefreshTokenRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val authApi: AlertCityApi
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_RETRIES = 2
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.contains("/api/v1/auth/")) {
            return null
        }

        if (responseCount(response) >= MAX_RETRIES) {
            Log.w(TAG, "Máximo de reintentos 401 alcanzado; cerrando sesión.")
            sessionManager.clearSession()
            return null
        }

        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

        synchronized(this) {
            val currentAccessToken = sessionManager.getAccessToken()

            // Si otro hilo ya renovó el token, reintentamos con el token actualizado
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.clearSession()
                return null
            }

            return try {
                val refreshResponse = authApi.refreshSync(RefreshTokenRequest(refreshToken)).execute()
                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newAuth = refreshResponse.body()!!
                    sessionManager.saveSession(newAuth)
                    Log.d(TAG, "Access token renovado silenciosamente.")
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newAuth.accessToken}")
                        .build()
                } else {
                    Log.w(TAG, "Refresh token inválido o expirado (${refreshResponse.code()}); cerrando sesión.")
                    sessionManager.clearSession()
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al renovar access token", e)
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

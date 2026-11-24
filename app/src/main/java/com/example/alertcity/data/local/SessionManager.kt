package com.example.alertcity.data.local

import android.content.Context
import com.example.alertcity.data.mapper.ReporteMapper
import com.example.alertcity.data.remote.model.AuthResponse
import com.example.alertcity.domain.model.Reporte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val email: String,
    val rol: String,
    val firstName: String,
    val lastName: String
) {
    val nombreCompleto: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { username }

    val rolNormalizado: String
        get() = rol.trim().uppercase()

    val esCiudadano: Boolean
        get() = rolNormalizado == "CIUDADANO"

    val esOperadorOAdmin: Boolean
        get() = !esCiudadano

    val puedeCambiarEstadoReportes: Boolean
        get() = esOperadorOAdmin

    /**
     * Devuelve la categoría del backend asignada a este rol operativo.
     * - Si es un rol departamental específico (ej. VIALIDAD, AGUA_DRENAJE), devuelve su categoría en mayúsculas.
     * - Si es ADMIN general o CIUDADANO, devuelve null.
     */
    val categoriaBackendAsignada: String?
        get() = when (rolNormalizado) {
            "VIALIDAD" -> "VIALIDAD"
            "ALUMBRADO" -> "ALUMBRADO"
            "AGUA_DRENAJE", "AGUA" -> "AGUA"
            "LIMPIEZA" -> "LIMPIEZA"
            "SEGURIDAD" -> "SEGURIDAD"
            "AREAS_VERDES" -> "AREAS_VERDES"
            "ANIMALES" -> "ANIMALES"
            else -> null
        }

    val categoriaUiAsignada: String?
        get() = categoriaBackendAsignada?.let { ReporteMapper.mapBackendCategoryToUi(it) }

    /**
     * Verifica si un operador/admin puede visualizar y gestionar un reporte según su rol específico.
     */
    fun puedeVerYGeestionarReporte(reporte: Reporte): Boolean {
        if (esCiudadano) return false
        val categoriaObligatoria = categoriaBackendAsignada ?: return true
        return ReporteMapper.mapCategoryToBackendEnum(reporte.categoria) == categoriaObligatoria
    }

    val rolLegible: String
        get() = when (rolNormalizado) {
            "CIUDADANO" -> "Ciudadano"
            "ADMIN" -> "Administrador General"
            "VIALIDAD" -> "Operador · Vialidad"
            "ALUMBRADO" -> "Operador · Alumbrado"
            "AGUA_DRENAJE", "AGUA" -> "Operador · Agua y Drenaje"
            "LIMPIEZA" -> "Operador · Limpieza"
            "SEGURIDAD" -> "Operador · Seguridad"
            "AREAS_VERDES" -> "Operador · Áreas Verdes"
            "ANIMALES" -> "Operador · Animales"
            else -> rol
        }
}

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionFlow = MutableStateFlow(loadSession())
    val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    val currentSession: UserSession?
        get() = _sessionFlow.value

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun saveSession(auth: AuthResponse) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, auth.accessToken)
            .putString(KEY_REFRESH_TOKEN, auth.refreshToken)
            .putString(KEY_USER_ID, auth.userId)
            .putString(KEY_USERNAME, auth.username)
            .putString(KEY_EMAIL, auth.email)
            .putString(KEY_ROL, auth.rol)
            .putString(KEY_FIRST_NAME, auth.firstName)
            .putString(KEY_LAST_NAME, auth.lastName)
            .apply()
        _sessionFlow.value = loadSession()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionFlow.value = null
    }

    private fun loadSession(): UserSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val rol = prefs.getString(KEY_ROL, "CIUDADANO") ?: "CIUDADANO"
        val firstName = prefs.getString(KEY_FIRST_NAME, "") ?: ""
        val lastName = prefs.getString(KEY_LAST_NAME, "") ?: ""

        return UserSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            username = username,
            email = email,
            rol = rol,
            firstName = firstName,
            lastName = lastName
        )
    }

    companion object {
        private const val PREFS_NAME = "alertcity_session_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROL = "rol"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
    }
}

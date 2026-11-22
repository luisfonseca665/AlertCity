package com.example.alertcity.data.remote.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

data class ApiErrorResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("instance") val instance: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("errors") val errors: Map<String, String>? = null
)

object ApiErrorParser {
    private val gson = Gson()

    fun parse(response: Response<*>): ApiErrorResponse? {
        val rawBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (rawBody.isNullOrBlank()) return null
        return try {
            gson.fromJson(rawBody, ApiErrorResponse::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun formatMessage(response: Response<*>, defaultMessage: String = "Error en la solicitud"): String {
        val parsed = parse(response)
        if (parsed != null) {
            val fieldErrors = parsed.errors
                ?.takeIf { it.isNotEmpty() }
                ?.entries
                ?.joinToString("\n") { (campo, msg) -> "• $campo: $msg" }

            return when {
                !fieldErrors.isNullOrBlank() -> {
                    val cabecera = parsed.detail ?: parsed.title ?: "Error de validación"
                    "$cabecera\n$fieldErrors"
                }
                !parsed.detail.isNullOrBlank() -> parsed.detail
                !parsed.title.isNullOrBlank() -> parsed.title
                else -> "$defaultMessage (HTTP ${response.code()})"
            }
        }

        return when (response.code()) {
            400 -> "Datos inválidos. Verifica los campos ingresados."
            401 -> "Sesión expirada o credenciales inválidas."
            403 -> "No tienes permisos para realizar esta acción."
            404 -> "Recurso no encontrado."
            409 -> "El usuario o correo electrónico ya está registrado."
            413 -> "El archivo excede el tamaño máximo permitido (10 MB)."
            else -> "$defaultMessage (HTTP ${response.code()})"
        }
    }
}

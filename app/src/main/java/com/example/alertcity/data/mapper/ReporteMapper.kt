package com.example.alertcity.data.mapper

import com.example.alertcity.data.local.entity.ReporteEntity
import com.example.alertcity.data.remote.model.CreateReportRequest
import com.example.alertcity.data.remote.model.ReportResponse
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.util.formatearCoordenadas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReporteMapper {

    fun mapCategoryToBackendEnum(categoriaUi: String): String {
        val limpia = categoriaUi.trim().uppercase()
        return when {
            limpia.contains("VIALIDAD") || limpia.contains("BACHE") -> "VIALIDAD"
            limpia.contains("ALUMBRADO") || limpia.contains("LUZ") -> "ALUMBRADO"
            limpia.contains("AGUA") || limpia.contains("FUGA") || limpia.contains("DRENAJE") -> "AGUA"
            limpia.contains("BASURA") || limpia.contains("LIMPIEZA") -> "LIMPIEZA"
            limpia.contains("SEGURIDAD") || limpia.contains("VANDALISMO") -> "SEGURIDAD"
            limpia.contains("PARQUE") || limpia.contains("VERDE") || limpia.contains("ÁREA") || limpia.contains("AREA") -> "AREAS_VERDES"
            limpia.contains("ANIMAL") -> "ANIMALES"
            else -> "OTRO"
        }
    }

    fun mapBackendCategoryToUi(categoriaBackend: String?): String {
        return when (categoriaBackend?.uppercase()?.trim()) {
            "VIALIDAD" -> "Baches y Vialidad"
            "ALUMBRADO" -> "Alumbrado Público"
            "AGUA", "AGUA_DRENAJE" -> "Fugas de Agua"
            "LIMPIEZA" -> "Basura y Limpieza"
            "SEGURIDAD" -> "Seguridad y Vandalismo"
            "AREAS_VERDES" -> "Parques y Áreas Verdes"
            "ANIMALES" -> "Animales en Abandono"
            "OTRO" -> "Otro"
            else -> categoriaBackend ?: "Otro"
        }
    }

    fun mapBackendStateToDomain(state: String?): EstadoReporte {
        return when (state?.uppercase()?.trim()) {
            "RESUELTO" -> EstadoReporte.RESUELTO
            "EN_PROCESO" -> EstadoReporte.EN_PROCESO
            "REVISION" -> EstadoReporte.REVISION
            else -> EstadoReporte.ENVIADO
        }
    }

    fun parseIsoDateTimeToTimestamp(isoString: String?): Pair<Long, String> {
        if (isoString.isNullOrBlank()) {
            val ahora = System.currentTimeMillis()
            val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ahora))
            return ahora to horaActual
        }

        val formatos = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        )

        for (formato in formatos) {
            try {
                val date = formato.parse(isoString)
                if (date != null) {
                    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                    return date.time to hora
                }
            } catch (_: Exception) { }
        }

        val ahora = System.currentTimeMillis()
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ahora))
        return ahora to horaActual
    }

    fun formatCoordinatesToLocationString(lat: Double?, lon: Double?, addressRef: String?): String? {
        if (lat != null && lon != null) {
            val coordsStr = formatearCoordenadas(lat, lon)
            return if (!addressRef.isNullOrBlank()) {
                "$coordsStr ($addressRef)"
            } else {
                coordsStr
            }
        }
        return addressRef
    }

    fun toDomain(response: ReportResponse): Reporte {
        val (timestamp, hora) = parseIsoDateTimeToTimestamp(response.dateTime)
        return Reporte(
            idReporte = response.reportId,
            idUsuario = response.userId ?: "11111111-1111-4111-a111-111111111111",
            titulo = response.title,
            descripcion = response.description,
            categoria = mapBackendCategoryToUi(response.category),
            esUrgente = response.urgent,
            hora = hora,
            fechaCreacion = timestamp,
            fotoUri = null,
            ubicacion = formatCoordinatesToLocationString(response.latitude, response.longitude, response.addressReference),
            estado = mapBackendStateToDomain(response.state)
        )
    }

    fun toEntity(response: ReportResponse): ReporteEntity {
        val domain = toDomain(response)
        return ReporteEntity(
            idReporte = domain.idReporte,
            idUsuario = domain.idUsuario,
            titulo = domain.titulo,
            descripcion = domain.descripcion,
            categoria = domain.categoria,
            esUrgente = domain.esUrgente,
            hora = domain.hora,
            fechaCreacion = domain.fechaCreacion,
            fotoUri = domain.fotoUri,
            ubicacion = domain.ubicacion,
            estado = domain.estado
        )
    }

    fun toCreateRequest(
        userId: String,
        titulo: String,
        descripcion: String,
        categoria: String,
        esUrgente: Boolean,
        latitud: Double?,
        longitud: Double?,
        referencia: String?
    ): CreateReportRequest {
        return CreateReportRequest(
            userId = userId,
            title = titulo,
            description = descripcion,
            category = mapCategoryToBackendEnum(categoria),
            urgent = esUrgente,
            latitude = latitud,
            longitude = longitud,
            addressReference = referencia,
            state = "ENVIADO"
        )
    }
}

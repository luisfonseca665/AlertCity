package com.example.alertcity.domain.util

import com.example.alertcity.domain.model.Reporte
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

const val LATITUD_CIUDAD_DEFECTO = 20.1387
const val LONGITUD_CIUDAD_DEFECTO = -101.1895

private val REGEX_NUMERO_COORD = Regex("[-+]?\\d+(?:\\.\\d+)?")

data class MarcadorReporte(
    val reporte: Reporte,
    val latitud: Double,
    val longitud: Double,
    val tieneUbicacionExacta: Boolean
)

fun formatearCoordenadas(latitud: Double, longitud: Double): String {
    val ns = if (latitud >= 0.0) "N" else "S"
    val ew = if (longitud >= 0.0) "E" else "W"
    return String.format(
        Locale.US,
        "%.5f° %s, %.5f° %s",
        abs(latitud),
        ns,
        abs(longitud),
        ew
    )
}

fun parsearCoordenadasUbicacion(ubicacion: String?): Pair<Double, Double>? {
    if (ubicacion.isNullOrBlank()) return null
    val partes = ubicacion.split(",")
    if (partes.size < 2) return null

    val latRaw = REGEX_NUMERO_COORD.find(partes[0])?.value?.toDoubleOrNull() ?: return null
    val lonRaw = REGEX_NUMERO_COORD.find(partes[1])?.value?.toDoubleOrNull() ?: return null

    val latitud = if (latRaw > 0 && partes[0].contains("S", ignoreCase = true)) -latRaw else latRaw
    val longitud = if (lonRaw > 0 && (partes[1].contains("W", ignoreCase = true) || partes[1].contains("O", ignoreCase = true))) {
        -lonRaw
    } else {
        lonRaw
    }

    if (latitud !in -90.0..90.0 || longitud !in -180.0..180.0) return null
    return latitud to longitud
}

fun construirMarcadoresReportes(reportes: List<Reporte>): List<MarcadorReporte> {
    val conteoPorCoordenada = mutableMapOf<String, Int>()

    return reportes.map { reporte ->
        val parseada = parsearCoordenadasUbicacion(reporte.ubicacion)
        val latBase = parseada?.first ?: LATITUD_CIUDAD_DEFECTO
        val lonBase = parseada?.second ?: LONGITUD_CIUDAD_DEFECTO
        val clave = "${"%.4f".format(Locale.US, latBase)},${"%.4f".format(Locale.US, lonBase)}"

        val indiceRepetido = conteoPorCoordenada.getOrDefault(clave, 0)
        conteoPorCoordenada[clave] = indiceRepetido + 1

        // Si varios reportes comparten la misma coordenada, se separan ligeramente en círculo para ver todos los pines
        val desplazamiento = if (indiceRepetido == 0) {
            0.0 to 0.0
        } else {
            val angulo = indiceRepetido * (Math.PI / 3.0)
            val radio = 0.0006 * ((indiceRepetido + 5) / 6)
            (radio * cos(angulo)) to (radio * sin(angulo))
        }

        MarcadorReporte(
            reporte = reporte,
            latitud = latBase + desplazamiento.first,
            longitud = lonBase + desplazamiento.second,
            tieneUbicacionExacta = parseada != null
        )
    }
}

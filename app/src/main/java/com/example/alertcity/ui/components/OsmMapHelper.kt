package com.example.alertcity.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.alertcity.R
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

const val LATITUD_CIUDAD_DEFECTO = 20.1387
const val LONGITUD_CIUDAD_DEFECTO = -101.1895

const val COLOR_PIN_RESUELTO = "#2E7D32"
const val COLOR_PIN_URGENTE = "#D32F2F"
const val COLOR_PIN_ACTIVO = "#1565C0"

private val REGEX_NUMERO_COORD = Regex("[-+]?\\d+(?:\\.\\d+)?")
private val cacheBitmapsPin = mutableMapOf<String, Bitmap>()

val TEMA_MAPA = XYTileSource(
    "AlertCity",
    1,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.tile.openstreetmap.fr/hot/",
        "https://b.tile.openstreetmap.fr/hot/",
        "https://c.tile.openstreetmap.fr/hot/"
    )
)

fun configurarOsmdroid(context: Context) {
    val baseDir = File(context.cacheDir, "alertcity_osm").apply { mkdirs() }
    val tileCacheDir = File(baseDir, "tiles_clean").apply { mkdirs() }
    Configuration.getInstance().apply {
        osmdroidBasePath = baseDir
        osmdroidTileCache = tileCacheDir
        userAgentValue = "AlertCityApp/1.0 (https://github.com/alertcity; contacto@alertcity.mx)"
    }
}

fun MapView.aplicarEstiloModerno() {
    setTileSource(TEMA_MAPA)
    isTilesScaledToDpi = true
    setMultiTouchControls(true)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
}

fun crearPinMapa(context: Context, colorHex: String): Drawable? {
    val claveCache = "${colorHex}_${context.resources.displayMetrics.densityDpi}"
    val bitmapExistente = cacheBitmapsPin[claveCache]
    if (bitmapExistente != null && !bitmapExistente.isRecycled) {
        return BitmapDrawable(context.resources, bitmapExistente)
    }

    val base = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)?.mutate() ?: return null
    val colorInt = android.graphics.Color.parseColor(colorHex)

    // MULTIPLY tiñe el cuerpo blanco del pin con el color exacto conservando el borde oscuro (#212121)
    DrawableCompat.setTintMode(base, PorterDuff.Mode.MULTIPLY)
    DrawableCompat.setTint(base, colorInt)

    val ancho = base.intrinsicWidth.coerceAtLeast(48)
    val alto = base.intrinsicHeight.coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    base.setBounds(0, 0, canvas.width, canvas.height)
    base.draw(canvas)

    // Centro blanco nítido del marcador
    val paintCentro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(ancho / 2f, alto * (9f / 24f), ancho * (2.3f / 24f), paintCentro)

    cacheBitmapsPin[claveCache] = bitmap
    return BitmapDrawable(context.resources, bitmap)
}

fun colorHexPin(
    estado: EstadoReporte = EstadoReporte.ENVIADO,
    esUrgente: Boolean
): String = when {
    estado == EstadoReporte.RESUELTO -> COLOR_PIN_RESUELTO
    esUrgente -> COLOR_PIN_URGENTE
    else -> COLOR_PIN_ACTIVO
}

fun colorHexPorReporte(reporte: Reporte): String =
    colorHexPin(estado = reporte.estado, esUrgente = reporte.esUrgente)

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

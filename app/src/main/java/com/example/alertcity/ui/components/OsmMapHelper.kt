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

const val COLOR_PIN_RESUELTO = "#2E7D32"
const val COLOR_PIN_URGENTE = "#D32F2F"
const val COLOR_PIN_ACTIVO = "#1565C0"

@Volatile
private var osmdroidConfigurado = false
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
    if (osmdroidConfigurado) return
    synchronized(TEMA_MAPA) {
        if (osmdroidConfigurado) return
        val appContext = context.applicationContext ?: context
        val baseDir = File(appContext.cacheDir, "alertcity_osm").apply { mkdirs() }
        val tileCacheDir = File(baseDir, "tiles_clean").apply { mkdirs() }
        Configuration.getInstance().apply {
            osmdroidBasePath = baseDir
            osmdroidTileCache = tileCacheDir
            userAgentValue = "AlertCityApp/1.0 (https://github.com/alertcity; contacto@alertcity.mx)"
        }
        osmdroidConfigurado = true
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

package com.example.alertcity.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

data class PinMapaUi(
    val id: String,
    val latitud: Double,
    val longitud: Double,
    val colorHex: String,
    val titulo: String? = null,
    val descripcion: String? = null,
    val mostrarInfoWindow: Boolean = false
)

private class MapRenderState {
    var ultimosPines: List<PinMapaUi>? = null
    var tieneEventosToque: Boolean = false
    var ultimaClaveCamara: String? = null
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun OsmMapCard(
    latitudCamara: Double,
    longitudCamara: Double,
    pines: List<PinMapaUi>,
    modifier: Modifier = Modifier,
    zoomInicial: Double = 15.5,
    zoomCamara: Double = zoomInicial,
    animarTrigger: Int = 0,
    alto: Dp = 220.dp,
    ocuparTodoElAlto: Boolean = false,
    radioEsquina: Dp = 16.dp,
    elevacion: Dp = 3.dp,
    etiquetaInferior: String? = null,
    onAmpliarClick: (() -> Unit)? = null,
    alineacionBotonAmpliar: Alignment = Alignment.TopEnd,
    onMapClick: ((Double, Double) -> Unit)? = null,
    onPinClick: ((String) -> Unit)? = null,
    contenidoSuperpuesto: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val estadoRenderizado = remember { MapRenderState() }

    val mapView = remember {
        configurarOsmdroid(context)
        MapView(context).apply {
            aplicarEstiloModerno()
            controller.setZoom(zoomInicial)
            controller.setCenter(GeoPoint(latitudCamara, longitudCamara))
            setOnTouchListener { vista, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        vista.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        vista.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val cardModifier = if (ocuparTodoElAlto) {
        modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxWidth()
            .height(alto)
    }

    ElevatedCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(radioEsquina),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevacion)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mapa ->
                    val necesitaEventosToque = currentOnMapClick != null
                    val cambiaronOverlays = estadoRenderizado.ultimosPines != pines ||
                        estadoRenderizado.tieneEventosToque != necesitaEventosToque

                    if (cambiaronOverlays) {
                        estadoRenderizado.ultimosPines = pines
                        estadoRenderizado.tieneEventosToque = necesitaEventosToque

                        InfoWindow.closeAllInfoWindowsOn(mapa)
                        mapa.overlays.clear()

                        if (necesitaEventosToque) {
                            val receptorToques = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p != null) {
                                        currentOnMapClick?.invoke(p.latitude, p.longitude)
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    if (p != null) {
                                        currentOnMapClick?.invoke(p.latitude, p.longitude)
                                    }
                                    return true
                                }
                            }
                            mapa.overlays.add(MapEventsOverlay(receptorToques))
                        }

                        pines.forEach { pin ->
                            val marker = Marker(mapa).apply {
                                position = GeoPoint(pin.latitud, pin.longitud)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = crearPinMapa(context, pin.colorHex)
                                title = pin.titulo
                                subDescription = pin.descripcion
                                if (currentOnPinClick != null) {
                                    setOnMarkerClickListener { m, _ ->
                                        m.showInfoWindow()
                                        currentOnPinClick?.invoke(pin.id)
                                        true
                                    }
                                }
                            }
                            mapa.overlays.add(marker)
                            if (pin.mostrarInfoWindow) {
                                marker.showInfoWindow()
                            }
                        }

                        mapa.invalidate()
                    }

                    val claveCamara = "$latitudCamara,$longitudCamara,$zoomCamara,$animarTrigger"
                    if (estadoRenderizado.ultimaClaveCamara != claveCamara) {
                        val esPrimeraCarga = estadoRenderizado.ultimaClaveCamara == null
                        estadoRenderizado.ultimaClaveCamara = claveCamara

                        if (animarTrigger > 0) {
                            mapa.controller.animateTo(
                                GeoPoint(latitudCamara, longitudCamara),
                                zoomCamara,
                                500L
                            )
                        } else if (!esPrimeraCarga) {
                            mapa.controller.setZoom(zoomCamara)
                            mapa.controller.setCenter(GeoPoint(latitudCamara, longitudCamara))
                        }
                    }
                }
            )

            if (onAmpliarClick != null) {
                Surface(
                    onClick = onAmpliarClick,
                    modifier = Modifier
                        .align(alineacionBotonAmpliar)
                        .padding(10.dp)
                        .size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Previsualizar mapa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (etiquetaInferior != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = etiquetaInferior,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            contenidoSuperpuesto()
        }
    }
}

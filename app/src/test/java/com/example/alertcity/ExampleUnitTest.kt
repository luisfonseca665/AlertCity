package com.example.alertcity

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.example.alertcity.data.local.UserSession
import com.example.alertcity.data.local.converter.ReporteConverters
import com.example.alertcity.data.local.entity.toDomain
import com.example.alertcity.data.local.entity.toEntity
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.model.SyncStatus
import com.example.alertcity.domain.util.GeoUtils
import com.example.alertcity.domain.util.parsearCoordenadasUbicacion
import com.example.alertcity.ui.components.AlertCityContentType
import com.example.alertcity.ui.components.AlertCityNavigationType
import com.example.alertcity.ui.components.COLOR_PIN_ACTIVO
import com.example.alertcity.ui.components.COLOR_PIN_RESUELTO
import com.example.alertcity.ui.components.COLOR_PIN_URGENTE
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.components.obtenerConfiguracionAdaptativa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun colorHexPorReporte_priorizaResueltoYUrgenteCorrectamente() {
        val reporteNormal = Reporte(
            titulo = "Lámpara apagada",
            descripcion = "No prende",
            categoria = "Alumbrado Público",
            esUrgente = false,
            hora = "20:00",
            estado = EstadoReporte.ENVIADO
        )
        val reporteUrgente = reporteNormal.copy(esUrgente = true)
        val reporteUrgenteResuelto = reporteUrgente.copy(estado = EstadoReporte.RESUELTO)

        assertEquals(COLOR_PIN_ACTIVO, colorHexPorReporte(reporteNormal))
        assertEquals(COLOR_PIN_URGENTE, colorHexPorReporte(reporteUrgente))
        assertEquals(COLOR_PIN_RESUELTO, colorHexPorReporte(reporteUrgenteResuelto))
    }

    @Test
    fun parsearCoordenadasUbicacion_convierteHemisferiosCorrectamente() {
        val coord = parsearCoordenadasUbicacion("20.1387° N, 101.1895° W")
        assertNotNull(coord)
        assertEquals(20.1387, coord!!.first, 0.0001)
        assertEquals(-101.1895, coord.second, 0.0001)
    }

    @Test
    fun ordenCronologico_usaFechaCreacionAntesQueHoraTexto() {
        val ayerNoche = Reporte(
            titulo = "Ayer",
            descripcion = "Desc",
            categoria = "Otro",
            esUrgente = false,
            hora = "23:50",
            fechaCreacion = 1_700_000_000_000L
        )
        val hoyManana = Reporte(
            titulo = "Hoy",
            descripcion = "Desc",
            categoria = "Otro",
            esUrgente = false,
            hora = "08:10",
            fechaCreacion = 1_700_086_400_000L
        )

        val ordenados = listOf(ayerNoche, hoyManana).sortedWith(
            compareByDescending<Reporte> { it.fechaCreacion }.thenByDescending { it.hora }
        )

        assertEquals("Hoy", ordenados.first().titulo)
        assertTrue(hoyManana.fechaHoraFormateada.contains("08:10"))
    }

    @Test
    fun haversineDistanceKm_calculaDistanciasConPrecision() {
        val mismoPunto = GeoUtils.haversineDistanceKm(20.1387, -101.1895, 20.1387, -101.1895)
        assertEquals(0.0, mismoPunto, 0.0001)

        // ~1.11 km por cada 0.01 grados de latitud
        val distanciaCorta = GeoUtils.haversineDistanceKm(20.1387, -101.1895, 20.1487, -101.1895)
        assertTrue("Distancia esperada ~1.11 km, fue $distanciaCorta", distanciaCorta in 1.0..1.2)

        // ~11.1 km por cada 0.1 grados de latitud (fuera de radio de 3 km)
        val distanciaLarga = GeoUtils.haversineDistanceKm(20.1387, -101.1895, 20.2387, -101.1895)
        assertTrue("Distancia esperada > 10 km, fue $distanciaLarga", distanciaLarga > 10.0)
    }

    @Test
    fun reporteConverters_convierteSyncStatusCorrectamente() {
        val converters = ReporteConverters()
        for (status in SyncStatus.entries) {
            val serialized = converters.fromSyncStatus(status)
            assertEquals(status, converters.toSyncStatus(serialized))
        }
        assertEquals(SyncStatus.SYNCED, converters.toSyncStatus("VALOR_DESCONOCIDO"))
    }

    @Test
    fun reporteEntityMapping_conservaCamposOfflineYSyncStatus() {
        val domain = Reporte(
            idReporte = "local-123",
            titulo = "Poste dañado",
            descripcion = "Sin luz en la cuadra",
            categoria = "Alumbrado Público",
            esUrgente = true,
            hora = "21:15",
            fotoUri = null,
            localFotoUri = "file:///data/user/0/app/files/reportes_fotos/foto_1.jpg",
            latitud = 20.1401,
            longitud = -101.1902,
            syncStatus = SyncStatus.SYNC_PENDING,
            syncError = null
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertEquals(SyncStatus.SYNC_PENDING, restored.syncStatus)
        assertEquals(20.1401, restored.latitud!!, 0.0001)
        assertEquals(-101.1902, restored.longitud!!, 0.0001)
        assertEquals("file:///data/user/0/app/files/reportes_fotos/foto_1.jpg", restored.localFotoUri)
        assertEquals("file:///data/user/0/app/files/reportes_fotos/foto_1.jpg", restored.fotoUri)
    }

    @Test
    fun rbacRoles_separaCiudadanoYFiltraReportesPorTipoDeProblema() {
        val reporteVialidad = Reporte(titulo = "Bache", descripcion = "Calle", categoria = "Baches y Vialidad", esUrgente = true, hora = "10:00")
        val reporteAgua = Reporte(titulo = "Fuga", descripcion = "Tubo", categoria = "Fugas de Agua", esUrgente = true, hora = "11:00")
        val reporteAlumbrado = Reporte(titulo = "Poste", descripcion = "Luz", categoria = "Alumbrado Público", esUrgente = false, hora = "12:00")

        val sesionCiudadano = UserSession("t", "r", "1", "laura", "l@a.com", "CIUDADANO", "Laura", "Sánchez")
        val sesionVialidad = UserSession("t", "r", "2", "carlos", "c@a.com", "VIALIDAD", "Carlos", "Vialidad")
        val sesionAgua = UserSession("t", "r", "3", "roberto", "r@a.com", "AGUA_DRENAJE", "Roberto", "Agua")

        assertTrue(sesionCiudadano.esCiudadano)
        assertFalse(sesionCiudadano.esOperadorOAdmin)
        assertFalse(sesionCiudadano.puedeVerYGeestionarReporte(reporteVialidad))

        assertTrue(sesionVialidad.esOperadorOAdmin)
        assertTrue(sesionVialidad.puedeVerYGeestionarReporte(reporteVialidad))
        assertFalse(sesionVialidad.puedeVerYGeestionarReporte(reporteAgua))
        assertFalse(sesionVialidad.puedeVerYGeestionarReporte(reporteAlumbrado))

        assertTrue(sesionAgua.esOperadorOAdmin)
        assertTrue(sesionAgua.puedeVerYGeestionarReporte(reporteAgua))
        assertFalse(sesionAgua.puedeVerYGeestionarReporte(reporteVialidad))
    }

    @Test
    fun windowSizeClass_asignaNavegacionYContenidoAdaptativoCorrectamente() {
        val compact = obtenerConfiguracionAdaptativa(WindowWidthSizeClass.Compact)
        assertEquals(AlertCityNavigationType.BOTTOM_NAVIGATION, compact.navigationType)
        assertEquals(AlertCityContentType.LIST_ONLY, compact.contentType)

        val medium = obtenerConfiguracionAdaptativa(WindowWidthSizeClass.Medium)
        assertEquals(AlertCityNavigationType.NAVIGATION_RAIL, medium.navigationType)
        assertEquals(AlertCityContentType.LIST_ONLY, medium.contentType)

        val expanded = obtenerConfiguracionAdaptativa(WindowWidthSizeClass.Expanded)
        assertEquals(AlertCityNavigationType.PERMANENT_NAVIGATION_DRAWER, expanded.navigationType)
        assertEquals(AlertCityContentType.LIST_AND_DETAIL, expanded.contentType)
    }
}

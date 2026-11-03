package com.example.alertcity

import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.ui.components.COLOR_PIN_ACTIVO
import com.example.alertcity.ui.components.COLOR_PIN_RESUELTO
import com.example.alertcity.ui.components.COLOR_PIN_URGENTE
import com.example.alertcity.ui.components.colorHexPorReporte
import com.example.alertcity.ui.components.parsearCoordenadasUbicacion
import org.junit.Assert.assertEquals
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
}

package com.example.alertcity.data.repository

import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class FakeReporteRepository : ReporteRepository {
    private val reportesEnMemoria = MutableStateFlow<List<Reporte>>(
        listOf(
            Reporte(titulo = "Bache enorme", descripcion = "En la calle principal", categoria = "Baches", esUrgente = true, hora = "10:00", estado = EstadoReporte.EN_PROCESO),
            Reporte(titulo = "Lámpara fundida", descripcion = "Frente al parque", categoria = "Alumbrado", esUrgente = false, hora = "14:30", estado = EstadoReporte.RESUELTO)
        )
    )

    override fun obtenerReportes(): Flow<List<Reporte>> = reportesEnMemoria

    override suspend fun guardarReporte(reporte: Reporte) {
        reportesEnMemoria.value = reportesEnMemoria.value + reporte
    }
}
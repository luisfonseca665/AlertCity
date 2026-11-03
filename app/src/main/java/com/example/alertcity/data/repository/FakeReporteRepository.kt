package com.example.alertcity.data.repository

import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeReporteRepository : ReporteRepository {
    companion object {
        private val reportesEnMemoria = MutableStateFlow(
            listOf(
                Reporte(titulo = "Bache enorme", descripcion = "En la calle principal, casi llegando a la avenida.", categoria = "Baches", esUrgente = true, hora = "10:00", estado = EstadoReporte.EN_PROCESO),
                Reporte(titulo = "Lámpara fundida", descripcion = "Frente al parque, la calle está muy oscura por las noches.", categoria = "Alumbrado", esUrgente = false, hora = "14:30", estado = EstadoReporte.RESUELTO),
                Reporte(titulo = "Fuga de agua", descripcion = "El tubo se rompió en la esquina.", categoria = "Fugas", esUrgente = true, hora = "08:15", estado = EstadoReporte.ENVIADO)
            )
        )
    }

    override fun getAllReports(): Flow<List<Reporte>> = reportesEnMemoria

    override suspend fun saveReport(report: Reporte) {
        reportesEnMemoria.update { listOf(report) + it }
    }
}

package com.example.alertcity.data.repository

import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

abstract class FakeReporteRepository : ReporteRepository {
    companion object {
        private val reportesEnMemoria = MutableStateFlow<List<Reporte>>(
            listOf(
                Reporte(titulo = "Bache enorme", descripcion = "En la calle principal, casi llegando a la avenida.", categoria = "Baches", esUrgente = true, hora = "10:00", estado = EstadoReporte.EN_PROCESO),
                Reporte(titulo = "Lámpara fundida", descripcion = "Frente al parque, la calle está muy oscura por las noches.", categoria = "Alumbrado", esUrgente = false, hora = "14:30", estado = EstadoReporte.RESUELTO),
                Reporte(titulo = "Fuga de agua", descripcion = "El tubo se rompió en la esquina.", categoria = "Fugas", esUrgente = true, hora = "08:15", estado = EstadoReporte.ENVIADO),
                Reporte(titulo = "Semáforo descompuesto", descripcion = "Semáforo parpadeando en rojo, causando tráfico.", categoria = "Vialidad", esUrgente = true, hora = "16:45", estado = EstadoReporte.EN_PROCESO),
                Reporte(titulo = "Basura acumulada", descripcion = "El camión de la basura no ha pasado en una semana.", categoria = "Limpieza", esUrgente = false, hora = "11:20", estado = EstadoReporte.REVISION),
                Reporte(titulo = "Árbol caído", descripcion = "Bloqueando el paso peatonal en la plaza.", categoria = "Parques", esUrgente = true, hora = "09:00", estado = EstadoReporte.RESUELTO),
                Reporte(titulo = "Alcantarilla sin tapa", descripcion = "Peligroso para los autos, ubicado en la glorieta.", categoria = "Vialidad", esUrgente = true, hora = "20:00", estado = EstadoReporte.EN_PROCESO),
                Reporte(titulo = "Grafiti en monumento", descripcion = "Se vandalizó la estatua del centro histórico.", categoria = "Seguridad", esUrgente = false, hora = "07:30", estado = EstadoReporte.ENVIADO)
            )
        )
    }

    override fun getAllReports(): Flow<List<Reporte>> = reportesEnMemoria

    override suspend fun saveReport(reporte: Reporte) {
        reportesEnMemoria.value = reportesEnMemoria.value + reporte
    }
}
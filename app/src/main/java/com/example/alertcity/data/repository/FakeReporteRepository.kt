package com.example.alertcity.data.repository

import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class FakeReporteRepository : ReporteRepository {
    private val reportesEnMemoria = MutableStateFlow<List<Reporte>>(emptyList())

    override fun obtenerReportes(): Flow<List<Reporte>> = reportesEnMemoria

    override suspend fun guardarReporte(reporte: Reporte) {
        reportesEnMemoria.value = reportesEnMemoria.value + reporte
    }
}
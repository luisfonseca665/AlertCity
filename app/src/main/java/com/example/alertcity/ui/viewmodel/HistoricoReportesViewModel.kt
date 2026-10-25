package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alertcity.data.repository.FakeReporteRepository
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

class HistoricoReportesViewModel(
    private val repository: ReporteRepository = FakeReporteRepository()
) : ViewModel() {
    
    val listaReportes = repository.obtenerReportes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun obtenerReportePorId(id: String): Reporte? {
        return listaReportes.value.find { it.id == id } ?: runBlocking {
            repository.obtenerReportes().first().find { it.id == id }
        }
    }
}
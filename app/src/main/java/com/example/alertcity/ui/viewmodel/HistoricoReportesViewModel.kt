package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.repository.FakeReporteRepository
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HistoricoReportesViewModel(
    private val repository: ReporteRepository
) : ViewModel() {
    
    val listaReportes = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun obtenerReportePorId(id: String): Reporte? {
        return listaReportes.value.find { it.idReporte == id }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                HistoricoReportesViewModel(app.reporteRepository)
            }
        }
    }
}
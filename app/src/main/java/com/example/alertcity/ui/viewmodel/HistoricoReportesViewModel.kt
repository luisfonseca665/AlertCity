package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoricoReportesUiState(
    val reportes: List<Reporte> = emptyList()
)

class HistoricoReportesViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricoReportesUiState())
    val uiState: StateFlow<HistoricoReportesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                _uiState.update { it.copy(reportes = lista) }
            }
        }
    }

    fun obtenerReportePorId(id: String): Reporte? {
        return _uiState.value.reportes.find { it.idReporte == id }
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

package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaReportesUiState(
    val reportes: List<Reporte> = emptyList(),
    val totalReportes: Int = 0,
    val totalUrgentes: Int = 0,
    val cargando: Boolean = false
)

class ReporteViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListaReportesUiState())
    val uiState: StateFlow<ListaReportesUiState> = _uiState.asStateFlow()

    init {
        // Observación reactiva constante de Room
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                _uiState.update {
                    it.copy(
                        reportes = lista,
                        totalReportes = lista.size,
                        totalUrgentes = lista.count { reporte -> reporte.esUrgente }
                    )
                }
            }
        }

        // Descarga inicial de reportes del Backend local
        refrescar()
    }

    fun refrescar() {
        if (_uiState.value.cargando) return
        viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            _uiState.update { it.copy(cargando = true) }
            repository.refreshReports()
            val transcurrido = System.currentTimeMillis() - inicio
            if (transcurrido < 350L) {
                delay(350L - transcurrido)
            }
            _uiState.update { it.copy(cargando = false) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                ReporteViewModel(app.reporteRepository)
            }
        }
    }
}

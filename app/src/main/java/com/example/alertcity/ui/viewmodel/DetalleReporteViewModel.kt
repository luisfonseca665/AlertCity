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

data class DetalleReporteUiState(
    val reporteId: String? = null,
    val reporte: Reporte? = null,
    val estuvoCargado: Boolean = false,
    val sincronizando: Boolean = false,
    val mensajeAccion: String? = null,
    val mostrarPreviewImagen: Boolean = false,
    val mostrarPreviewMapa: Boolean = false
)

class DetalleReporteViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleReporteUiState())
    val uiState: StateFlow<DetalleReporteUiState> = _uiState.asStateFlow()

    private var todosLosReportes: List<Reporte> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                todosLosReportes = lista
                val idActual = _uiState.value.reporteId
                if (idActual != null) {
                    val encontrado = lista.find { it.idReporte == idActual }
                    _uiState.update { actual ->
                        actual.copy(
                            reporte = encontrado,
                            estuvoCargado = actual.estuvoCargado || encontrado != null
                        )
                    }
                }
            }
        }
    }

    fun cargarReporte(reporteId: String?) {
        val encontrado = reporteId?.let { id -> todosLosReportes.find { it.idReporte == id } }
        _uiState.update { actual ->
            if (actual.reporteId == reporteId && actual.reporte == encontrado) {
                actual
            } else {
                actual.copy(
                    reporteId = reporteId,
                    reporte = encontrado,
                    estuvoCargado = actual.estuvoCargado || encontrado != null,
                    mostrarPreviewImagen = false,
                    mostrarPreviewMapa = false
                )
            }
        }
    }

    fun onMostrarPreviewImagenChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarPreviewImagen = mostrar) }
    }

    fun onMostrarPreviewMapaChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarPreviewMapa = mostrar) }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensajeAccion = null) }
    }

    fun reintentarSincronizacion() {
        if (_uiState.value.sincronizando) return
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true, mensajeAccion = null) }
            val result = repository.syncPendingReports()
            result.fold(
                onSuccess = { synced ->
                    repository.refreshReports()
                    _uiState.update {
                        it.copy(
                            sincronizando = false,
                            mensajeAccion = if (synced > 0) {
                                "Reporte sincronizado con el servidor"
                            } else {
                                "No hay reportes pendientes por sincronizar"
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            sincronizando = false,
                            mensajeAccion = error.localizedMessage ?: "No se pudo sincronizar el reporte"
                        )
                    }
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                DetalleReporteViewModel(repository = app.reporteRepository)
            }
        }
    }
}

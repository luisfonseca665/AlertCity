package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.domain.util.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.LONGITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.MarcadorReporte
import com.example.alertcity.domain.util.construirMarcadoresReportes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoricoReportesUiState(
    val reportes: List<Reporte> = emptyList(),
    val marcadores: List<MarcadorReporte> = emptyList(),
    val reporteSeleccionado: Reporte? = null,
    val latitudCentro: Double = LATITUD_CIUDAD_DEFECTO,
    val longitudCentro: Double = LONGITUD_CIUDAD_DEFECTO,
    val zoomMapa: Double = 15.0,
    val enfocarTrigger: Int = 0,
    val modoCercanos: Boolean = false,
    val radioKm: Double = 3.0,
    val mostrarPreviewMapa: Boolean = false,
    val cargando: Boolean = false,
    val mensajeError: String? = null
)

class HistoricoReportesViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricoReportesUiState())
    val uiState: StateFlow<HistoricoReportesUiState> = _uiState.asStateFlow()

    private var todosLosReportes: List<Reporte> = emptyList()

    init {
        viewModelScope.launch {
            repository.refreshReports()
        }
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                todosLosReportes = lista
                if (!_uiState.value.modoCercanos) {
                    actualizarListaYMarcadores(lista)
                }
            }
        }
    }

    fun cargarReportesCercanos(latitud: Double, longitud: Double, radiusKm: Double = 3.0) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    cargando = true,
                    mensajeError = null,
                    modoCercanos = true,
                    radioKm = radiusKm
                )
            }
            val result = repository.getNearbyReports(lat = latitud, lng = longitud, radiusKm = radiusKm)
            result.fold(
                onSuccess = { cercanos ->
                    val marcadoresCalculados = construirMarcadoresReportes(cercanos)
                    _uiState.update { actual ->
                        actual.copy(
                            cargando = false,
                            reportes = cercanos,
                            marcadores = marcadoresCalculados,
                            reporteSeleccionado = null,
                            latitudCentro = latitud,
                            longitudCentro = longitud,
                            zoomMapa = 14.5,
                            enfocarTrigger = actual.enfocarTrigger + 1
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { actual ->
                        actual.copy(
                            cargando = false,
                            mensajeError = error.localizedMessage ?: "No se pudieron cargar reportes cercanos"
                        )
                    }
                }
            )
        }
    }

    fun mostrarTodos() {
        _uiState.update { it.copy(modoCercanos = false, mensajeError = null) }
        actualizarListaYMarcadores(todosLosReportes)
    }

    private fun actualizarListaYMarcadores(lista: List<Reporte>) {
        val marcadoresCalculados = construirMarcadoresReportes(lista)
        _uiState.update { actual ->
            val primerMarcador = marcadoresCalculados.firstOrNull()
            val mantenerCentro = actual.enfocarTrigger > 0 && actual.marcadores.isNotEmpty()
            val seleccionadoActualizado = actual.reporteSeleccionado?.let { sel ->
                lista.find { it.idReporte == sel.idReporte }
            }
            actual.copy(
                reportes = lista,
                marcadores = marcadoresCalculados,
                reporteSeleccionado = seleccionadoActualizado,
                latitudCentro = if (mantenerCentro) actual.latitudCentro else (primerMarcador?.latitud ?: LATITUD_CIUDAD_DEFECTO),
                longitudCentro = if (mantenerCentro) actual.longitudCentro else (primerMarcador?.longitud ?: LONGITUD_CIUDAD_DEFECTO),
                enfocarTrigger = if (!mantenerCentro && primerMarcador != null) actual.enfocarTrigger + 1 else actual.enfocarTrigger
            )
        }
    }

    fun refrescar() {
        if (_uiState.value.cargando) return
        viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            repository.refreshReports()
            val transcurrido = System.currentTimeMillis() - inicio
            if (transcurrido < 350L) {
                delay(350L - transcurrido)
            }
            _uiState.update { it.copy(cargando = false) }
        }
    }

    fun seleccionarReporteEnMapa(idReporte: String) {
        val marcador = _uiState.value.marcadores.find { it.reporte.idReporte == idReporte } ?: return
        _uiState.update {
            it.copy(
                reporteSeleccionado = marcador.reporte,
                latitudCentro = marcador.latitud,
                longitudCentro = marcador.longitud,
                zoomMapa = 17.0,
                enfocarTrigger = it.enfocarTrigger + 1
            )
        }
    }

    fun limpiarSeleccionMapa() {
        _uiState.update { it.copy(reporteSeleccionado = null) }
    }

    fun onMostrarPreviewMapaChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarPreviewMapa = mostrar) }
    }

    fun centrarVistaGeneral() {
        val primerMarcador = _uiState.value.marcadores.firstOrNull()
        _uiState.update {
            it.copy(
                reporteSeleccionado = null,
                latitudCentro = primerMarcador?.latitud ?: LATITUD_CIUDAD_DEFECTO,
                longitudCentro = primerMarcador?.longitud ?: LONGITUD_CIUDAD_DEFECTO,
                zoomMapa = 15.0,
                enfocarTrigger = it.enfocarTrigger + 1
            )
        }
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
